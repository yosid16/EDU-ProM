package org.eduprom.miners.adaptiveNoise;

/**
 * Created by ydahari on 9/22/2017.
 */
public class ConformanceInfo {
    private Double fitness;
    private Double precision;

    private double precisionWeight;
    private double fitnessWeight;

    ConformanceInfo(double fitnessWeight, double precisionWeight){
        this.fitnessWeight = fitnessWeight;
        this.precisionWeight = precisionWeight;
    }

    public Double getFitness() {
        return fitness;
    }

    public void setFitness(Double fitness) {
        this.fitness = fitness;
    }

    public Double getPrecision() {
        return precision;
    }

    public void setPrecision(Double precision) {
        this.precision = precision;
    }

    public double getPsi(){
        return fitnessWeight * fitness + precisionWeight * precision;
    }

    public double getFitnessWeight() {
        return fitnessWeight;
    }

    public double getPrecisionWeight() {
        return precisionWeight;
    }
}
