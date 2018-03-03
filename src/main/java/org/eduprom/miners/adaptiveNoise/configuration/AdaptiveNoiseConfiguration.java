package org.eduprom.miners.adaptiveNoise.configuration;

import org.eduprom.benchmarks.configuration.Weights;
import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner;
import org.eduprom.partitioning.ILogSplitter;

public class AdaptiveNoiseConfiguration {


    //region private members

    private float[] noiseThresholds;
    private final float partitionNoiseFilter;
    private Weights weights;
    private boolean useCrossValidation;
    private boolean preExecuteFilter;
    private Class<? extends ILogSplitter> logSplitter;

    //endregoin

    //region builder class
    public static class AdaptiveNoiseConfigurationBuilder {

        private float[] noiseThresholds;

        private Weights weights;
        private float partitionNoiseFilter;

        private boolean useCrossValidation;
        private boolean preExecuteFilter;
        private Class<? extends ILogSplitter> logSplitter;

        public AdaptiveNoiseConfigurationBuilder setNoiseThresholds(float... noiseThresholds){
            this.noiseThresholds = noiseThresholds;
            return this;
        }

        public AdaptiveNoiseConfigurationBuilder setWeights(double fitnessWeight, double precisionWeight, double generalizationWeight){
            this.weights = new Weights(fitnessWeight, precisionWeight, generalizationWeight);
            return this;
        }

        public AdaptiveNoiseConfigurationBuilder setWeights(Weights weights){
            this.weights = weights;
            return this;
        }

        public AdaptiveNoiseConfigurationBuilder setWeights(){
            this.weights = Weights.getUniform();
            return this;
        }


        public AdaptiveNoiseConfigurationBuilder useCrossValidation(boolean useCrossValidation){
            this.useCrossValidation = useCrossValidation;
            return this;
        }

        public AdaptiveNoiseConfigurationBuilder setPartitionNoiseFilter(float partitionNoiseFilter) {
            this.partitionNoiseFilter = partitionNoiseFilter;
            return this;
        }

        public AdaptiveNoiseConfigurationBuilder setPreExecuteFilter(boolean preExecuteFilter) {
            this.preExecuteFilter = preExecuteFilter;
            return this;
        }

        public AdaptiveNoiseConfigurationBuilder setLogSplitter(Class<? extends ILogSplitter> logSplitter) {
            this.logSplitter = logSplitter;
            return this;
        }

        public float[] getNoiseThresholds() {
            return noiseThresholds;
        }

        public boolean getUseCrossValidation() {
            return useCrossValidation;
        }

        public AdaptiveNoiseConfiguration build(){
            return new AdaptiveNoiseConfiguration(this);
        }

        public float getPartitionNoiseFilter() {
            return partitionNoiseFilter;
        }

        public Weights getWeights(){
            return this.weights;
        }

        public boolean isPreExecuteFilter() {
            return preExecuteFilter;
        }

        public Class<? extends ILogSplitter> getLogSplitter() {
            return logSplitter;
        }
    }
    //endregion

    private AdaptiveNoiseConfiguration(AdaptiveNoiseConfigurationBuilder builder){
        this.noiseThresholds = builder.getNoiseThresholds();
        this.weights = builder.getWeights();
        this.useCrossValidation = builder.getUseCrossValidation();
        this.partitionNoiseFilter = builder.getPartitionNoiseFilter();
        this.preExecuteFilter = builder.isPreExecuteFilter();
        this.logSplitter = builder.getLogSplitter();
    }

    public float[] getNoiseThresholds() {
        return noiseThresholds;
    }

    public Weights getWeights() {
        return weights;
    }

    public boolean getUseCrossValidation(){
        return useCrossValidation;
    }

    public float getPartitionNoiseFilter() {
        return partitionNoiseFilter;
    }

    public static AdaptiveNoiseConfigurationBuilder getBuilder(){
        return new AdaptiveNoiseConfigurationBuilder();
    }


    public AdaptiveNoiseMiner getMiner(String filename) throws Exception {
        return new AdaptiveNoiseMiner(filename, this);
    }

    public boolean isPreExecuteFilter() {
        return preExecuteFilter;
    }

    public Class<? extends ILogSplitter> getLogSplitter() {
        return logSplitter;
    }
}
