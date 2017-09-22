package org.eduprom.miners.adaptiveNoise;

import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;

import java.util.UUID;

public class Change {

    private UUID id;
    private NoiseInductiveMiner miner;

    public Change(UUID id, NoiseInductiveMiner miner){
        this.id = id;
        this.miner = miner;
    }

    public UUID getId(){
        return this.id;
    }

    public NoiseInductiveMiner getMiner(){
        return this.miner;
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
