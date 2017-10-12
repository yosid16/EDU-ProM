package org.eduprom.miners.adaptiveNoise;

import org.eduprom.exceptions.MiningException;

/**
 * Created by ydahari on 9/22/2017.
 */
public class ConformanceInfo {
    private Double fitness;
    private Double precision;
    private Double generalization;

    private double precisionWeight;
    private double fitnessWeight;
    private Double generalizationWeight;


    public ConformanceInfo(double fitnessWeight, double precisionWeight, double generalizationWeight){
        this.fitnessWeight = fitnessWeight;
        this.precisionWeight = precisionWeight;
        this.generalizationWeight = generalizationWeight;
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
    public double getGeneralization(){
        return this.generalization;
    }

    public void setGeneralization(Double generalization){
        this.generalization = generalization;
    }

    public double getPsi() {
        if (!assigned()){
            int a = 1;
            //throw new MiningException("associated quality metric has not been assigned");
        }

        return fitnessWeight * fitness + precisionWeight * precision + generalizationWeight * generalization;
    }

    public double getFitnessWeight() {
        return fitnessWeight;
    }

    public double getPrecisionWeight() {
        return precisionWeight;
    }

    public double getGeneralizationWeight(){
        return this.generalizationWeight;
    }

    public boolean assigned() {
        return fitness != null || precision != null || generalization != null;
    }

    @Override
    public String toString() {
        return String.format("fitness (%f, %f), precision(%f, %f), generalization: (%f,%f)",
                fitnessWeight, fitness, precisionWeight, precision, generalizationWeight, generalization);
    }
}
