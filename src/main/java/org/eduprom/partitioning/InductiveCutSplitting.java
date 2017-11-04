package org.eduprom.partitioning;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.eduprom.miners.AbstractMiner;
import org.eduprom.miners.adaptiveNoise.conformance.IConformanceContext;
import org.processmining.framework.packages.PackageManager;
import org.processmining.plugins.InductiveMiner.conversion.ReduceTree;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTreeReduce;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.processmining.plugins.InductiveMiner.mining.IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.MinerState;
import org.processmining.plugins.InductiveMiner.mining.MinerStateBase;
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
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;

import java.util.Iterator;
import java.util.logging.Logger;


public class InductiveCutSplitting implements ILogSplitter {

    protected final static Logger logger = Logger.getLogger(AbstractMiner.class.getName());

    private IConformanceContext conformanceContext;
    private MiningParametersLogSplitting parameters;

    protected static PackageManager.Canceller canceller = new PackageManager.Canceller() {

        @Override
        public boolean isCancelled() {
            // This for passing the canceller to ProM interface
            // We don't cancel since we do not work interactively
            return false;
        }
    };

    public InductiveCutSplitting(IConformanceContext conformanceContext, float noiseThreshold) {
        this.conformanceContext = conformanceContext;
        parameters = new MiningParametersLogSplitting() {{ setNoiseThreshold(noiseThreshold); }};
    }

    @Override
    public Partitioning split(XLog xLog) {
        Partitioning partitioning = new Partitioning(conformanceContext, xLog);
        this.parameters.setLogPartitining(partitioning);
        parameters.getPostProcessors().add(new PostProcessorPartitioning());
        IMProcessTree.mineProcessTree(xLog, this.parameters, canceller);
        partitioning.setProcessTree(partitioning.getPartitions().values().stream().findAny().get().getNode().getProcessTree());
        return partitioning;
    }
}
