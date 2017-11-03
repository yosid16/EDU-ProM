package org.eduprom.miners.adaptiveNoise;

import org.eduprom.exceptions.MiningException;

/**
 * Created by ydahari on 9/22/2017.
 */
public class ConformanceInfo {
    private long fitnessDuration;
    private long precisionDuration;
    private long generalizationDuration;


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
        return fitness != null && precision != null && generalization != null;
    }

    @Override
    public String toString() {
        return String.format("psi: %f, fitness (%f, %f), precision(%f, %f), generalization: (%f,%f)", getPsi(),
                fitnessWeight, fitness, precisionWeight, precision, generalizationWeight, generalization);
    }

    public double maxValue(){
        return fitnessWeight * (fitness == null ? 1 : fitness) +
                precisionWeight * (precision == null ? 1 : precision) +
                generalizationWeight * (generalization == null ? 1 : generalization);
    }

    public double minValue(){
        return fitnessWeight * (fitness == null ? 0 : fitness) +
                precisionWeight * (precision == null ? 0 : precision) +
                generalizationWeight * (generalization == null ? 0 : generalization);
    }

    public ConformanceInfo CloneWeights(){
        return new ConformanceInfo(this.getFitnessWeight(), this.getPrecisionWeight(), this.getGeneralizationWeight());
    }

    public long getFitnessDuration() {
        return fitnessDuration;
    }

    public void setFitnessDuration(long fitnessDuration) {
        this.fitnessDuration = fitnessDuration;
    }

    public long getPrecisionDuration() {
        return precisionDuration;
    }

    public void setPrecisionDuration(long precisionDuration) {
        this.precisionDuration = precisionDuration;
    }

    public long getGeneralizationDuration() {
        return generalizationDuration;
    }

    public void setGeneralizationDuration(long generalizationDuration) {
        this.generalizationDuration = generalizationDuration;
    }
}
