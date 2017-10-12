package org.eduprom.miners.adaptiveNoise.conformance;

import org.deckfour.xes.model.XLog;
import org.eduprom.miners.adaptiveNoise.ConformanceInfo;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.MiningResult;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.processtree.ProcessTree;

import java.util.UUID;

public interface IConformanceObject {

    UUID getId();

    XLog getLog();

    NoiseInductiveMiner getMiner();

    void setMiningResult(MiningResult result);

    MiningResult getMiningResult();

    void setConformanceInfo(ConformanceInfo conformanceInfo);

    ConformanceInfo getConformanceInfo();
}
