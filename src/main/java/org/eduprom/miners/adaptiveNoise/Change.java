package org.eduprom.miners.adaptiveNoise;

import org.deckfour.xes.model.XLog;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.MiningResult;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.jbpt.petri.untangling.Process;
import org.processmining.processtree.ProcessTree;

import java.util.UUID;

public class Change {

    private UUID id;
    private NoiseInductiveMiner miner;
    private XLog log;
    private MiningResult result;
    private ProcessTree processTree;
    private int bitsChanged;

    public Change(UUID id, XLog log, NoiseInductiveMiner miner){
        this.id = id;
        this.log = log;
        this.miner = miner;
    }

    public UUID getId(){
        return this.id;
    }

    public NoiseInductiveMiner getMiner(){
        return this.miner;
    }

    public ProcessTree getProcessTree(){
        discover();
        return this.result.getProcessTree();
    }

    public int getBitsChanged(){
        discover();
        return this.result.getFilterResult().getBitsRemoved();
    }

    public void discover(){
        if (result == null){
            this.result = miner.mineProcessTree(log, id);
        }
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
        return String.format("id: %s, noise: %f", id.toString(), miner.getNoiseThreshold());
    }
}
