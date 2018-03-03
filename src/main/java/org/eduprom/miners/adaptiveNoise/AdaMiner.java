package org.eduprom.miners.adaptiveNoise;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.eduprom.benchmarks.IBenchmarkableMiner;
import org.eduprom.benchmarks.configuration.NoiseThreshold;
import org.eduprom.benchmarks.configuration.Weights;
import org.eduprom.entities.CrossValidationPartition;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.miners.AbstractMiner;
import org.eduprom.miners.AbstractPetrinetMiner;
import org.eduprom.miners.InductiveMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.MiningResult;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.miners.adaptiveNoise.benchmarks.AdaBenchmark;
import org.eduprom.miners.adaptiveNoise.configuration.AdaptiveNoiseConfiguration;
import org.eduprom.miners.adaptiveNoise.conformance.ConformanceInfo;
import org.eduprom.miners.adaptiveNoise.conformance.IAdaptiveNoiseConformanceObject;
import org.eduprom.miners.adaptiveNoise.conformance.IConformanceContext;
import org.eduprom.miners.adaptiveNoise.filters.FilterAlgorithm;
import org.eduprom.miners.adaptiveNoise.filters.FilterResult;
import org.eduprom.partitioning.ILogSplitter;
import org.eduprom.partitioning.MiningParametersLogSplitting;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.framework.packages.PackageManager;
import org.processmining.log.parameters.LowFrequencyFilterParameters;
import org.processmining.plugins.InductiveMiner.conversion.ReduceTree;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTreeReduce;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.processmining.plugins.InductiveMiner.mining.IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.MinerState;
import org.processmining.plugins.InductiveMiner.mining.MinerStateBase;
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
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.impl.ProcessTreeImpl;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner.FITNESS_KEY;


public class AdaMiner extends AbstractPetrinetMiner implements IBenchmarkableMiner {

    protected final static Logger logger = Logger.getLogger(AbstractMiner.class.getName());

    private Map<Float, MinerState> parametersIMfMap;
    private MiningParametersIMf parameters;
    private AdaptiveNoiseConfiguration adaptiveNoiseConfiguration;
    private ProcessTree bestTree;
    private ConformanceInfo conformanceInfo;

    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings minePetrinet() throws MiningException {
        return this.petrinetHelper.ConvertToPetrinet(discover(this.log));
    }


    protected static PackageManager.Canceller _canceller = new PackageManager.Canceller() {

        @Override
        public boolean isCancelled() {
            // This for passing the canceller to ProM interface
            // We don't cancel since we do not work interactively
            return false;
        }
    };

    public AdaMiner(String filename, AdaptiveNoiseConfiguration adaptiveNoiseConfiguration) throws LogFileNotFoundException {
        super(filename);
        //this.noiseThreshold = noiseThreshold;
        this.parametersIMfMap = new HashMap<>();
        this.adaptiveNoiseConfiguration = adaptiveNoiseConfiguration;
        float[] thresholds = adaptiveNoiseConfiguration.getNoiseThresholds();
        this.parameters = new MiningParametersIMf();
        for (float threshold: thresholds) {
            this.parametersIMfMap.put(threshold, new MinerState(new MiningParametersIMf() {{
                setNoiseThreshold(threshold);
            }}, _canceller));
        }
    }


    public ProcessTree discover(XLog xlog) throws MiningException {
        logger.info(String.format("Miners with %d noise thresholds %s are optional",
                adaptiveNoiseConfiguration.getNoiseThresholds().length, this.parametersIMfMap.keySet().stream()
                        .map(x-> String.valueOf(x.floatValue())).collect(Collectors.joining (","))));
        Weights weights = adaptiveNoiseConfiguration.getWeights();
        logger.info(format("Fitness weight: %f, Precision weight: %f, generalization weight: %f",
                weights.getFitnessWeight(), weights.getPrecisionWeight(), weights.getGeneralizationWeight()));

        IMLog log = new IMLogImpl(xlog, new XEventNameClassifier());
        //repair life cycle if necessary
        if (this.parameters.isRepairLifeCycle()) {
            log = LifeCycles.preProcessLog(log);
        }

        //create process tree

        ProcessTree tree = new ProcessTreeImpl();
        MinerState minerState = new MinerState(this.parameters, _canceller);
        Node root = mineNode(log, tree, minerState);

        if (_canceller.isCancelled()) {
            minerState.shutdownThreadPools();
            return null;
        }

        root.setProcessTree(tree);
        tree.setRoot(root);
        discoveredTrees.putIfAbsent(tree.toString(), tree);


        logger.info(String.format("Found total %d trees", discoveredTrees.size()));


        Map<ProcessTree, ConformanceInfo> treeConformanceInfoEntry = discoveredTrees.values().stream().collect(Collectors.toMap(x->x, x -> {
            try {
                return AdaBenchmark.getPsi(petrinetHelper, x, xlog, xlog, weights);
            } catch (MiningException e) {
                throw new RuntimeException();
            }
        }));

        for(String treeRepresentation : discoveredTrees.keySet()){
            logger.info(String.format("discovered tree: %s", treeRepresentation));
        }
        Map.Entry<ProcessTree, ConformanceInfo> bestModel = treeConformanceInfoEntry.entrySet().stream()
                .max(Comparator.comparing(x->x.getValue().getPsi())).get();
        this.bestTree = bestModel.getKey();
        this.conformanceInfo = bestModel.getValue();
        logger.info(String.format("Best AdA model: conformance %s, tree: %s", bestModel.getValue(), bestModel.getKey().toString()));


        if (_canceller.isCancelled()) {
            minerState.shutdownThreadPools();
            return null;
        }

        debug("discovered tree " + bestModel.getKey().getRoot(), minerState);

        //reduce the tree
        if (this.parameters.getReduceParameters() != null) {
            try {
                this.bestTree = ReduceTree.reduceTree(this.bestTree, this.parameters.getReduceParameters());
                debug("after reduction " + tree.getRoot(), minerState);
            } catch (UnknownTreeNodeException | EfficientTreeReduce.ReductionFailedException e) {
                e.printStackTrace();
            }
        }

        minerState.shutdownThreadPools();

        if (_canceller.isCancelled()) {
            return null;
        }

        return this.bestTree;
    }

    private Map<String, ProcessTree> discoveredTrees = new HashMap<>();

    private Map.Entry<Float, MinerState> obtainMinerState(IMLog log) throws MiningException {
        ConformanceInfo bestCutConformanceInfo = null;
        Map.Entry<Float, MinerState> bestCut = null;
        for(Map.Entry<Float, MinerState> mfEntry: parametersIMfMap.entrySet()) {
            NoiseInductiveMiner miner = new NoiseInductiveMiner(filename, mfEntry.getKey(), adaptiveNoiseConfiguration.isPreExecuteFilter());
            XLog cLog = log.toXLog();
            /*
            int partitionSize = (int)Math.round(log.size() / 10.0);
            if (partitionSize == 0){
                partitionSize = 1;
            }
            List<CrossValidationPartition> origin =  this.logHelper.crossValidationSplit(cLog, partitionSize);
            CrossValidationPartition[] validationPartitions = CrossValidationPartition.take(origin, 1);
            origin = CrossValidationPartition.exclude(origin, validationPartitions);

            XLog validationLog = CrossValidationPartition.bind(validationPartitions).getLog();
            XLog trainingLog = partitionSize > 1 ? CrossValidationPartition.bind(origin).getLog() : validationLog;
            */

            List<CrossValidationPartition> origin =  this.logHelper.crossValidationSplit(cLog, 10);
            CrossValidationPartition[] validationPartitions = CrossValidationPartition.take(origin, 1);
            origin = CrossValidationPartition.exclude(origin, validationPartitions);

            XLog validationLog = CrossValidationPartition.bind(validationPartitions).getLog();
            XLog trainingLog = CrossValidationPartition.bind(origin).getLog();

            ProcessTree subLogTree = miner.mineProcessTree(trainingLog).getProcessTree();
            //logger.info(String.format("evaluating conformance for noise threshold: %f", mfEntry.getKey()));
            ConformanceInfo conformanceInfo = AdaBenchmark.getPsi(petrinetHelper, subLogTree, trainingLog, validationLog, adaptiveNoiseConfiguration.getWeights());
            //logger.info(String.format("finished evaluating conformance for noise threshold: %f", mfEntry.getKey()));
            logger.info(String.format("%f threshold, conformance info: %s, tree: %s",  mfEntry.getKey(), conformanceInfo, subLogTree.toString()));
            //discoveredTrees.putIfAbsent(subLogTree.toString(), subLogTree);

            if (bestCut == null || conformanceInfo.getPsi() > bestCutConformanceInfo.getPsi()){
                bestCutConformanceInfo = conformanceInfo;
                bestCut = mfEntry;
            }
        }

        return bestCut;
    }

    public Node mineNode(IMLog log, ProcessTree tree, MinerState minerState) throws MiningException {
        //construct basic information about log
        IMLogInfo logInfo = minerState.parameters.getLog2LogInfo().createLogInfo(log);

        //output information about the log
        debug("\nmineProcessTree epsilon=" + logInfo.getDfg().getNumberOfEmptyTraces() + ", " + logInfo.getActivities(),
                minerState);
        //debug(log, minerState);

        //find base cases
        Node baseCase = findBaseCases(log, logInfo, tree, minerState);
        if (baseCase != null) {

            baseCase = postProcess(baseCase, log, logInfo, minerState);

            debug(" discovered node " + baseCase, minerState);
            return baseCase;
        }

        if (minerState.isCancelled()) {
            return null;
        }

        Map.Entry<Float, MinerState> bestCut = obtainMinerState(log);
        logger.info("started evaluating miners");

        logger.info(String.format("Best cut of %f noise threshold", bestCut.getKey()));
        Cut cut = findCut(log, logInfo, bestCut.getValue());
        return handleCut(bestCut.getValue(), cut, logInfo, log, tree);
    }

    private Node handleCut(MinerState minerState, Cut cut, IMLogInfo logInfo, IMLog log, ProcessTree tree) throws MiningException {
        if (minerState.isCancelled()) {
            return null;
        }

        if (cut != null && cut.isValid()) {
            //cut is valid

            debug(" chosen cut: " + cut, minerState);

            //split logs
            LogSplitter.LogSplitResult splitResult = splitLog(log, logInfo, cut, minerState);

            if (minerState.isCancelled()) {
                return null;
            }

            //make node
            Block newNode;
            try {
                newNode = newNode(cut.getOperator());
            } catch (UnknownTreeNodeException e) {
                e.printStackTrace();
                return null;
            }
            addNode(tree, newNode);

            //recurse
            if (cut.getOperator() != Cut.Operator.loop) {
                for (IMLog sublog : splitResult.sublogs) {
                    Node child = mineNode(sublog, tree, minerState);

                    if (minerState.isCancelled()) {
                        return null;
                    }

                    addChild(newNode, child, minerState);
                }
            } else {
                //loop needs special treatment:
                //ProcessTree requires a ternary loop
                Iterator<IMLog> it = splitResult.sublogs.iterator();

                //mine body
                IMLog firstSublog = it.next();
                {
                    Node firstChild = mineNode(firstSublog, tree, minerState);

                    if (minerState.isCancelled()) {
                        return null;
                    }

                    addChild(newNode, firstChild, minerState);
                }

                //mine redo parts by, if necessary, putting them under an xor
                Block redoXor;
                if (splitResult.sublogs.size() > 2) {
                    redoXor = new AbstractBlock.Xor("");
                    addNode(tree, redoXor);
                    addChild(newNode, redoXor, minerState);
                } else {
                    redoXor = newNode;
                }
                while (it.hasNext()) {
                    IMLog sublog = it.next();
                    Node child = mineNode(sublog, tree, minerState);

                    if (minerState.isCancelled()) {
                        return null;
                    }

                    addChild(redoXor, child, minerState);
                }

                //add tau as third child
                {
                    Node tau = new AbstractTask.Automatic("tau");
                    addNode(tree, tau);
                    addChild(newNode, tau, minerState);
                }
            }

            Node result = postProcess(newNode, log, logInfo, minerState);

            debug(" discovered node " + result, minerState);
            return result;

        } else {
            Map.Entry<Float, MinerState> fallThroughMinerState = obtainMinerState(log);
            logger.info(String.format("FallThrough noise: %f", fallThroughMinerState.getKey()));
            //cut is not valid; fall through
            Node result = findFallThrough(log, logInfo, tree, fallThroughMinerState.getValue());

            result = postProcess(result, log, logInfo, fallThroughMinerState.getValue());

            debug(" discovered node " + result, fallThroughMinerState.getValue());
            return result;
        }
    }

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
        logger.info(String.format("Fall Through: %s", n));
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

    public static void debug(Object x, MinerState minerState) {
        if (minerState.parameters.isDebug()) {
            System.out.println(x.toString());
        }
    }

    public static void addChild(Block parent, Node child, MinerStateBase minerState) {
        if (!minerState.isCancelled() && parent != null && child != null) {
            parent.addChild(child);
        }
    }

    @Override
    public ConformanceInfo getConformanceInfo() {
        return this.conformanceInfo;
    }

    @Override
    public void setConformanceInfo(ConformanceInfo conformanceInfo) {
        this.conformanceInfo = conformanceInfo;
    }

    @Override
    public ProcessTree2Petrinet.PetrinetWithMarkings getModel() {
        return this.getDiscoveredPetriNet();
    }

    @Override
    public PetrinetHelper getHelper() {
        return this.petrinetHelper;
    }

    @Override
    public ProcessTree getProcessTree() {
        return this.bestTree;
    }
}
