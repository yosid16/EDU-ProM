package org.eduprom.miners.adaptiveNoise.conformance;

import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.processmining.framework.plugin.PluginContext;

/**
 * Created by ydahari on 10/7/2017.
 */
public interface IConformanceContext {

    NoiseInductiveMiner getPartitionMiner();

    PluginContext getPluginContext();
}
