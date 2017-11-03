package org.eduprom.miners.adaptiveNoise.benchmarks;

import org.eduprom.benchmarks.Weights;
import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner;
import org.eduprom.miners.adaptiveNoise.configuration.AdaptiveNoiseConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AdaptiveNoiseBenchmarkConfiguration {


    //region private members

    private Float[] noiseThresholds;
    private final float partitionNoiseFilter;
    private List<Weights> weights;
    private boolean useCrossValidation;

    //endregoin

    //region builder class
    public static class AdaptiveNoiseBenchmarkConfigurationBuilder {

        private Float[] noiseThresholds;

        private List<Weights> weights;
        private float partitionNoiseFilter;

        private boolean useCrossValidation;

        public AdaptiveNoiseBenchmarkConfigurationBuilder(){
            this.weights = new ArrayList<>();
        }

        public AdaptiveNoiseBenchmarkConfigurationBuilder setNoiseThresholds(Float... noiseThresholds){
            this.noiseThresholds = noiseThresholds;
            return this;
        }

        public AdaptiveNoiseBenchmarkConfigurationBuilder addWeights(double fitnessWeight, double precisionWeight, double generalizationWeight){
            this.weights.add(new Weights(fitnessWeight, precisionWeight, generalizationWeight));
            return this;
        }

        public AdaptiveNoiseBenchmarkConfigurationBuilder addWeights(Weights weights){
            this.weights.add(weights);
            return this;
        }

        public AdaptiveNoiseBenchmarkConfigurationBuilder addWeights(Collection<Weights> weights){
            this.weights.addAll(weights);
            return this;
        }

        public AdaptiveNoiseBenchmarkConfigurationBuilder addWeights(){
            this.weights.add(Weights.getUniform());
            return this;
        }


        public AdaptiveNoiseBenchmarkConfigurationBuilder useCrossValidation(boolean useCrossValidation){
            this.useCrossValidation = useCrossValidation;
            return this;
        }

        public AdaptiveNoiseBenchmarkConfigurationBuilder setPartitionNoiseFilter(float partitionNoiseFilter) {
            this.partitionNoiseFilter = partitionNoiseFilter;
            return this;
        }

        public Float[] getNoiseThresholds() {
            return noiseThresholds;
        }

        public boolean getUseCrossValidation() {
            return useCrossValidation;
        }

        public AdaptiveNoiseBenchmarkConfiguration build(){
            return new AdaptiveNoiseBenchmarkConfiguration(this);
        }

        public float getPartitionNoiseFilter() {
            return partitionNoiseFilter;
        }

        public List<Weights> getWeights(){
            return this.weights;
        }
    }
    //endregion

    private AdaptiveNoiseBenchmarkConfiguration(AdaptiveNoiseBenchmarkConfigurationBuilder builder){
        this.noiseThresholds = builder.getNoiseThresholds();
        this.weights = builder.getWeights();
        this.useCrossValidation = builder.getUseCrossValidation();
        this.partitionNoiseFilter = builder.getPartitionNoiseFilter();
    }

    public Float[] getNoiseThresholds() {
        return noiseThresholds;
    }

    public List<Weights> getWeights() {
        return weights;
    }

    public boolean getUseCrossValidation(){
        return useCrossValidation;
    }

    public float getPartitionNoiseFilter() {
        return partitionNoiseFilter;
    }

    public static AdaptiveNoiseBenchmarkConfigurationBuilder getBuilder(){
        return new AdaptiveNoiseBenchmarkConfigurationBuilder();
    }

    /*
    public AdaptiveNoiseMiner getMiner(String filename) throws Exception {
        return new AdaptiveNoiseMiner(filename, this);
    }
    */

    public AdaptiveNoiseConfiguration getAdaptiveNoiseConfiguration(Weights weights){
        return AdaptiveNoiseConfiguration.getBuilder()
                .setNoiseThresholds(this.getNoiseThresholds())
                .setPartitionNoiseFilter(this.partitionNoiseFilter)
                .setWeights(weights)
                .build();
    }
}
