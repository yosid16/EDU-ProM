package org.eduprom.benchmarks.configuration;

import java.util.ArrayList;
import java.util.List;

public class Weights {
    private static final double TOTAL_WEIGHT = 1.0;
    private static final double TOTAL_MEASURES = 3.0;
    private static final double UNIFORM_WEIGHT = TOTAL_WEIGHT / TOTAL_MEASURES;

    private double precisionWeight;
    private double fitnessWeight;
    private double generalizationWeight;

    public Weights(double fitnessWeight, double precisionWeight, double generalizationWeight){
        this.precisionWeight = precisionWeight;
        this.fitnessWeight = fitnessWeight;
        this.generalizationWeight = generalizationWeight;
    }

    public double getPrecisionWeight() {
        return precisionWeight;
    }

    public double getFitnessWeight() {
        return fitnessWeight;
    }

    public double getGeneralizationWeight() {
        return generalizationWeight;
    }

    public static Weights getUniform(){
        return new Weights(UNIFORM_WEIGHT, UNIFORM_WEIGHT, UNIFORM_WEIGHT);
    }

    @Override
    public String toString() {
        return String.format("fitnessWeight: %f, precisionWeight: %f, generalizationWeight: %f",
                fitnessWeight, precisionWeight, generalizationWeight);
    }

    public static List<Weights> getRange(double interval){
        List<Weights> weightsList = new ArrayList<>();
        double fitness = TOTAL_WEIGHT;
        while(fitness >= 0){
            for(double precision = 0.0; precision <= TOTAL_WEIGHT - fitness; precision += interval){
                double generalization = TOTAL_WEIGHT - fitness - precision;
                weightsList.add(new Weights(fitness, precision, generalization));
            }

            fitness -= interval;
        }

        return weightsList;
    }


    public static List<Weights> getRangePrecision(double interval){
        List<Weights> weightsList = new ArrayList<>();
        double precision = TOTAL_WEIGHT;
        while(precision >= 0){
            weightsList.add(new Weights((TOTAL_WEIGHT - precision) / 2.0, precision, (TOTAL_WEIGHT - precision) / 2.0));
            precision -= interval;
        }

        return weightsList;
    }


}
