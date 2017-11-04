package org.eduprom.partitioning;

//import org.eduprom.partitioning.trunk.FallThroughPartitioningIM;
import org.processmining.plugins.InductiveMiner.dfgOnly.log2logInfo.IMLog2IMLogInfoDefault;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinder;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinderIM;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinderIMi;
import org.processmining.plugins.InductiveMiner.mining.cuts.CutFinder;
import org.processmining.plugins.InductiveMiner.mining.cuts.IM.CutFinderIM;
import org.processmining.plugins.InductiveMiner.mining.cuts.IMf.CutFinderIMf;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThrough;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThroughIM;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.*;
import org.processmining.plugins.InductiveMiner.mining.postprocessor.PostProcessor;
import org.processmining.plugins.InductiveMiner.mining.postprocessor.PostProcessorInterleaved;

import java.util.ArrayList;
import java.util.Arrays;

public class MiningParametersLogSplitting extends MiningParametersIMf {

    private Partitioning logPartitining;

    public MiningParametersLogSplitting() {

        setLog2LogInfo(new IMLog2IMLogInfoDefault());

        setBaseCaseFinders(new ArrayList<BaseCaseFinder>(Arrays.asList(
                new BaseCaseFinderIMi(),
                new BaseCaseFinderIM()
        )));

        setCutFinder(new ArrayList<CutFinder>(Arrays.asList(
                new CutFinderIM(),
                new CutFinderIMf()
        )));

        setLogSplitter(new LogSplitterCombination(
                new LogSplitterXorFiltering(),
                new LogSplitterSequenceFiltering(),
                new LogSplitterParallel(),
                new LogSplitterLoop(),
                new LogSplitterMaybeInterleaved(),
                new LogSplitterParallel(),
                new LogSplitterOr()));

        setFallThroughs(new ArrayList<FallThrough>(Arrays.asList(
                new FallThroughIM()
        )));

        setPostProcessors(new ArrayList<PostProcessor>(Arrays.asList(
                new PostProcessorInterleaved()
        )));

        //set parameters
        setNoiseThreshold((float) 0.0);

        getReduceParameters().setReduceToOr(false);
    }

    public Partitioning getLogPartitining() {
        return logPartitining;
    }

    public void setLogPartitining(Partitioning logPartitining) {
        this.logPartitining = logPartitining;
    }
}
