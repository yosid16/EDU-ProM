package org.eduprom.miners.adaptiveNoise.configuration;

import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner;

public class AdaptiveNoiseConfiguration {

    //region private members

    private Float[] noiseThresholds;

    private double precisionWeight;
    private double fitnessWeight;
    private Double generalizationWeight;
    private boolean useCrossValidation;

    //endregoin

    //region builder class
    public static class AdaptiveNoiseConfigurationBuilder {

        private Float[] noiseThresholds;

        private double precisionWeight;
        private double fitnessWeight;
        private double generalizationWeight;

        private boolean useCrossValidation;

        public AdaptiveNoiseConfigurationBuilder setNoiseThresholds(Float... noiseThresholds){
            this.noiseThresholds = noiseThresholds;
            return this;
        }

        public AdaptiveNoiseConfigurationBuilder setFitnessWeight(double fitnessWeight){
            this.fitnessWeight = fitnessWeight;
            return this;
        }

        public AdaptiveNoiseConfigurationBuilder setPrecisionWeight(double precisionWeight){
            this.precisionWeight = precisionWeight;
            return this;
        }

        public AdaptiveNoiseConfigurationBuilder setGeneralizationWeight(double generalizationWeight){
            this.generalizationWeight = generalizationWeight;
            return this;
        }

        public AdaptiveNoiseConfigurationBuilder useCrossValidation(boolean useCrossValidation){
            this.useCrossValidation = useCrossValidation;
            return this;
        }

        public Float[] getNoiseThresholds() {
            return noiseThresholds;
        }

        public double getPrecisionWeight() {
            return precisionWeight;
        }

        public double getFitnessWeight() {
            return fitnessWeight;
        }

        public Double getGeneralizationWeight() {
            return generalizationWeight;
        }

        public boolean getUseCrossValidation() {
            return useCrossValidation;
        }

        public AdaptiveNoiseConfiguration build(){
            return new AdaptiveNoiseConfiguration(this);
        }
    }
    //endregion

    private AdaptiveNoiseConfiguration(AdaptiveNoiseConfigurationBuilder builder){
        this.noiseThresholds = builder.getNoiseThresholds();
        this.fitnessWeight = builder.getFitnessWeight();
        this.precisionWeight = builder.getPrecisionWeight();
        this.generalizationWeight = builder.getGeneralizationWeight();
        this.useCrossValidation = builder.getUseCrossValidation();
    }

    public Float[] getNoiseThresholds() {
        return noiseThresholds;
    }


    public double getPrecisionWeight() {
        return precisionWeight;
    }

    public double getFitnessWeight() {
        return fitnessWeight;
    }


    public Double getGeneralizationWeight() {
        return generalizationWeight;
    }

    public boolean getUseCrossValidation(){
        return useCrossValidation;
    }


    public static AdaptiveNoiseConfigurationBuilder getBuilder(){
        return new AdaptiveNoiseConfigurationBuilder();
    }


    public AdaptiveNoiseMiner getMiner(String filename) throws Exception {
        return new AdaptiveNoiseMiner(filename, this);
    }
}
