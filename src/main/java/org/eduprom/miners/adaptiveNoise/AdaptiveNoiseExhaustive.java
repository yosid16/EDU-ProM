package org.eduprom.miners.adaptiveNoise;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.eduprom.benchmarks.configuration.NoiseThreshold;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.miners.AbstractMiner;
import org.eduprom.miners.AbstractPetrinetMiner;
import org.eduprom.miners.adaptiveNoise.conformance.IConformanceContext;
import org.eduprom.miners.adaptiveNoise.filters.FilterAlgorithm;
import org.eduprom.miners.adaptiveNoise.filters.FilterResult;
import org.eduprom.partitioning.ILogSplitter;
import org.eduprom.partitioning.MiningParametersLogSplitting;
import org.eduprom.partitioning.Partitioning;
import org.processmining.framework.packages.PackageManager;
import org.processmining.log.parameters.LowFrequencyFilterParameters;
import org.processmining.plugins.InductiveMiner.conversion.ReduceTree;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTreeReduce;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.processmining.plugins.InductiveMiner.mining.IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.MinerState;
import org.processmining.plugins.InductiveMiner.mining.MinerStateBase;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinder;
import org.processmining.plugins.InductiveMiner.mining.cuts.Cut;
import org.processmining.plugins.InductiveMiner.mining.cuts.CutFinder;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThrough;
import org.processmining.plugins.InductiveMiner.mining.interleaved.Interleaved;
import org.processmining.plugins.InductiveMiner.mining.interleaved.MaybeInterleaved;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitter;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLogImpl;
import org.processmining.plugins.InductiveMiner.mining.logs.LifeCycles;
import org.processmining.plugins.InductiveMiner.mining.postprocessor.PostProcessor;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.impl.ProcessTreeImpl;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;


public class AdaptiveNoiseExhaustive extends AbstractPetrinetMiner {

    protected final static Logger logger = Logger.getLogger(AbstractMiner.class.getName());

    //private NoiseThreshold noiseThreshold;
    private Map<Float, MinerState> parametersIMfMap;
    private MiningParametersIMf parameters;

    protected static PackageManager.Canceller _canceller = new PackageManager.Canceller() {

        @Override
        public boolean isCancelled() {
            // This for passing the canceller to ProM interface
            // We don't cancel since we do not work interactively
            return false;
        }
    };

    public AdaptiveNoiseExhaustive(String filename, NoiseThreshold noiseThreshold) throws LogFileNotFoundException {
        super(filename);
        //this.noiseThreshold = noiseThreshold;
        this.parametersIMfMap = new HashMap<>();
        float[] thresholds = noiseThreshold.getThresholds();
        this.parameters = new MiningParametersIMf();
        for (float threshold: thresholds) {
            this.parametersIMfMap.put(threshold,new MinerState(new MiningParametersIMf() {{ setNoiseThreshold(threshold);}}, _canceller));
        }
    }

    public ProcessTree discover(XLog xlog) {
        //XLog filteredLog = filterLog(xlog).getFilteredLog();
        //Partitioning partitioning = new Partitioning(conformanceContext, filteredLog);
        //this.parameters.setLogPartitining(partitioning);
        IMLog log = new IMLogImpl(xlog, new XEventNameClassifier());
        //repair life cycle if necessary
        if (this.parameters.isRepairLifeCycle()) {
            log = LifeCycles.preProcessLog(log);
        }

        //create process tree



        MinerState minerState = new MinerState(this.parameters, _canceller);
        List<ProcessTree> trees = mineNode(log, new ProcessTreeImpl(), minerState)
                .stream().map(Node::getProcessTree).collect(Collectors.toList());

        if (_canceller.isCancelled()) {
            minerState.shutdownThreadPools();
            return null;
        }


        if (_canceller.isCancelled()) {
            minerState.shutdownThreadPools();
            return null;
        }

        //reduce the tree
        //TODO: handle reduce parameters

        minerState.shutdownThreadPools();

        if (_canceller.isCancelled()) {
            return null;
        }

        return trees.stream().findAny().get();
    }

    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings minePetrinet() throws MiningException {
        return this.petrinetHelper.ConvertToPetrinet(discover(this.log));
    }

    public List<Node> mineNode(IMLog log, ProcessTree tree, MinerState minerState) {
        //construct basic information about log
        IMLogInfo logInfo = minerState.parameters.getLog2LogInfo().createLogInfo(log);

        //debug(log, minerState);

        //region base cases

        Node baseCase = findBaseCases(log, logInfo, tree, minerState);
        if (baseCase != null) {
            Node node = postProcess(baseCase, log, logInfo, minerState);
            return new ArrayList<Node>() {{ add(node); }};
        }

        //endregion
        ArrayList<Node> nodes = new ArrayList<Node>();
        for(Map.Entry<Float, MinerState> mfEntry: parametersIMfMap.entrySet()) {
            Cut cut = findCut(log, logInfo, mfEntry.getValue());
            ProcessTree clonedTree = tree.getRoot() != null ? tree.toTree() : tree;
            List<Node> cutNode = handleCut(log, logInfo, cut, mfEntry.getValue(), tree);
            nodes.addAll(cutNode);
        }

        return nodes;
    }

    private Block getNewNode(ProcessTree tree, Cut cut){
        //make node
        Block newNode;
        try {
            newNode = newNode(cut.getOperator());
        } catch (UnknownTreeNodeException e) {
            e.printStackTrace();
            return null;
        }
        addNode(tree, newNode);
        return newNode;
    }

    private List<Node> handleCut(IMLog log, IMLogInfo logInfo, Cut cut, MinerState minerState, ProcessTree originalTree) {
        if (minerState.isCancelled()) {
            return null;
        }

        if (cut != null && cut.isValid()) {
            //cut is valid

            //region split log by cut, add node to tree

            //split logs
            LogSplitter.LogSplitResult splitResult = splitLog(log, logInfo, cut, minerState);

            if (minerState.isCancelled()) {
                return null;
            }


            //endregion

            List<Node> nodes = new ArrayList<>();
            //recurse
            if (cut.getOperator() != Cut.Operator.loop) {

                //region valid cut, no loop
                for (IMLog sublog : splitResult.sublogs) {
                    for(Node child : mineNode(sublog, originalTree, minerState)){
                        ProcessTree tree = originalTree.getRoot() != null ? originalTree.toTree() : new ProcessTreeImpl();
                        Block newNode = getNewNode(tree, cut);
                        if (minerState.isCancelled()) {
                            return null;
                        }

                        addChild(newNode, child, minerState);
                        nodes.add(child);
                    }
                }
                //endregion
            } else {

                //region loop

                //loop needs special treatment:
                //ProcessTree requires a ternary loop
                Iterator<IMLog> it = splitResult.sublogs.iterator();

                IMLog firstSublog = it.next();
                for(Node firstChild : mineNode(firstSublog, originalTree, minerState)){
                    ProcessTree tree = originalTree.getRoot() != null ? originalTree.toTree() : new ProcessTreeImpl();
                    //mine redo parts by, if necessary, putting them under an xor
                    while (it.hasNext()) {
                        IMLog sublog = it.next();
                        List<Node> child = mineNode(sublog, tree, minerState);

                        if (minerState.isCancelled()) {
                            return null;
                        }
                        Block newNode = getNewNode(tree, cut);
                        Node node = mineLoop(firstChild, child, newNode, minerState, cut, splitResult);
                        nodes.add(node);
                    }
                }

                //endregion
            }
            nodes.forEach(x-> postProcess(x, log, logInfo, minerState));
            return nodes;

        } else {

            //region cut is not valid; fall through
            ProcessTree tree = originalTree.getRoot() != null ? originalTree.toTree() : new ProcessTreeImpl();
            Node result = findFallThrough(log, logInfo, tree, minerState);

            result = postProcess(result, log, logInfo, minerState);

            ArrayList<Node> res = new ArrayList<>();
            res.add(result);
            return res;

            //endregion
        }
    }

    private Node mineLoop(Node firstChild, List<Node> second, Block newNode, MinerState minerState, Cut cut, LogSplitter.LogSplitResult splitResult){


        addChild(newNode, firstChild, minerState);


        //mine redo parts by, if necessary, putting them under an xor
        Block redoXor;
        if (splitResult.sublogs.size() > 2) {
            redoXor = new AbstractBlock.Xor("");
            addNode(newNode.getProcessTree(), redoXor);
            addChild(newNode, redoXor, minerState);
        } else {
            redoXor = newNode;
        }

        for (Node child : second) {
            addChild(redoXor, child, minerState);
        }

        //add tau as third child
        {
            Node tau = new AbstractTask.Automatic("tau");
            addNode(newNode.getProcessTree(), tau);
            addChild(newNode, tau, minerState);
        }
        return newNode;
    }


    //region inductive methods

    private static Node postProcess(Node newNode, IMLog log, IMLogInfo logInfo, MinerState minerState) {
        for (PostProcessor processor : minerState.parameters.getPostProcessors()) {
            newNode = processor.postProcess(newNode, log, logInfo, minerState);
        }

        return newNode;
    }

    private static Block newNode(Cut.Operator operator) throws UnknownTreeNodeException {
        switch (operator) {
            case loop :
                return new AbstractBlock.XorLoop("");
            case concurrent :
                return new AbstractBlock.And("");
            case sequence :
                return new AbstractBlock.Seq("");
            case xor :
                return new AbstractBlock.Xor("");
            case maybeInterleaved :
                return new MaybeInterleaved("");
            case interleaved :
                return new Interleaved("");
            case or :
                return new AbstractBlock.Or("");
        }
        throw new UnknownTreeNodeException();
    }

    /**
     *
     * @param tree
     * @param node
     *            The log used as input for the mining algorithm. Provide null
     *            if this node was not directly derived from a log (e.g. it is a
     *            child in a flower-loop).
     */
    public static void addNode(ProcessTree tree, Node node) {
        node.setProcessTree(tree);
        tree.addNode(node);
    }

    public static Node findBaseCases(IMLog log, IMLogInfo logInfo, ProcessTree tree, MinerState minerState) {
        Node n = null;
        Iterator<BaseCaseFinder> it = minerState.parameters.getBaseCaseFinders().iterator();
        while (n == null && it.hasNext()) {

            if (minerState.isCancelled()) {
                return null;
            }

            n = it.next().findBaseCases(log, logInfo, tree, minerState);
        }
        return n;
    }

    public static Cut findCut(IMLog log, IMLogInfo logInfo, MinerState minerState) {
        Cut c = null;
        Iterator<CutFinder> it = minerState.parameters.getCutFinders().iterator();
        while (it.hasNext() && (c == null || !c.isValid())) {

            if (minerState.isCancelled()) {
                return null;
            }

            c = it.next().findCut(log, logInfo, minerState);
        }
        return c;
    }

    public static Node findFallThrough(IMLog log, IMLogInfo logInfo, ProcessTree tree, MinerState minerState) {
        Node n = null;
        Iterator<FallThrough> it = minerState.parameters.getFallThroughs().iterator();
        while (n == null && it.hasNext()) {

            if (minerState.isCancelled()) {
                return null;
            }

            n = it.next().fallThrough(log, logInfo, tree, minerState);
        }
        return n;
    }

    public static LogSplitter.LogSplitResult splitLog(IMLog log, IMLogInfo logInfo, Cut cut, MinerState minerState) {
        LogSplitter.LogSplitResult result = minerState.parameters.getLogSplitter().split(log, logInfo, cut, minerState);

        if (minerState.isCancelled()) {
            return null;
        }

        //merge the discarded events of this log splitting into the global discarded events list
        minerState.discardedEvents.addAll(result.discardedEvents);

        return result;
    }

    public static void addChild(Block parent, Node child, MinerStateBase minerState) {
        if (!minerState.isCancelled() && parent != null && child != null) {
            parent.addChild(child);
        }
    }

    //endregion
}
