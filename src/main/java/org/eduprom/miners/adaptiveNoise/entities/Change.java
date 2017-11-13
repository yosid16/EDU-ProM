package org.eduprom.miners.adaptiveNoise.entities;

import org.deckfour.xes.model.XLog;
import org.eduprom.exceptions.MiningException;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.MiningResult;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.miners.adaptiveNoise.conformance.ConformanceInfo;
import org.eduprom.miners.adaptiveNoise.conformance.IConformanceContext;
import org.eduprom.miners.adaptiveNoise.conformance.IAdaptiveNoiseConformanceObject;
import org.eduprom.miners.adaptiveNoise.entities.TreeChanges;
import org.eduprom.partitioning.Partitioning;
import org.processmining.processtree.ProcessTree;

import java.util.Set;
import java.util.UUID;

public class Change implements IAdaptiveNoiseConformanceObject {

    private Partitioning.PartitionInfo partitionInfo;
    private NoiseInductiveMiner miner;
    private XLog log;
    private MiningResult result;
    private ConformanceInfo info;
    private IConformanceContext conformanceContext;


    public Change(Partitioning.PartitionInfo partitionInfo, XLog log, NoiseInductiveMiner miner, IConformanceContext conformanceContext){
        this.partitionInfo = partitionInfo;
        this.log = log;
        this.miner = miner;
        this.conformanceContext = conformanceContext;
    }


    public UUID getId(){
        return this.partitionInfo.getId();
    }

    public Partitioning.PartitionInfo getPartitionInfo(){
        return this.partitionInfo;
    }

    @Override
    public XLog getLog() {
        return this.log;
    }

    public NoiseInductiveMiner getMiner(){
        return this.miner;
    }

    @Override
    public void setMiningResult(MiningResult result) {
        this.result = result;
    }

    @Override
    public MiningResult getMiningResult() {
        return this.result;
    }

    @Override
    public void setConformanceInfo(ConformanceInfo conformanceInfo) {
        this.info = conformanceInfo;
    }

    public ProcessTree getProcessTree() throws MiningException {
        return this.result.getProcessTree();
    }

    public int getBitsRemoved() {
        return this.result.getFilterResult().getBitsRemoved();
    }

    public int getBits() {
        return this.result.getFilterResult().getBits();
    }

    public ConformanceInfo getConformanceInfo(){
        return this.info;
    }

    public Set<UUID> getReplacedNodes(){
        return this.getPartitionInfo().getChildren();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof TreeChanges) && toString().equals(((TreeChanges)o).toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return String.format("SubTree: T_%d, noise: %f", getPartitionInfo().getSequentialId(), miner.getNoiseThreshold());
    }
}
