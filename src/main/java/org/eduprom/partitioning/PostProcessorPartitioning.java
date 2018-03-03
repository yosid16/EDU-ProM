package org.eduprom.partitioning;

import org.processmining.plugins.InductiveMiner.mining.IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.MinerState;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.InductiveMiner.mining.postprocessor.PostProcessor;
import org.processmining.processtree.Node;

public class PostProcessorPartitioning implements PostProcessor {
    @Override
    public Node postProcess(Node node, IMLog log, IMLogInfo logInfo, MinerState minerState) {
        Partitioning partitioning = ((MiningParametersLogSplitting)minerState.parameters).getLogPartitining();
        partitioning.add(node, log.toXLog());
        return node;
    }
}
