package org.eduprom.miners.adaptiveNoise.benchmarks;

import org.eduprom.benchmarks.configuration.Logs;
import org.eduprom.benchmarks.configuration.NoiseThreshold;
import org.eduprom.benchmarks.configuration.Weights;
import org.eduprom.miners.adaptiveNoise.configuration.AdaptiveNoiseConfiguration;
import org.eduprom.partitioning.ILogSplitter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AdaptiveNoiseBenchmarkConfiguration {


    //region private members

    private float[] noiseThresholds;
    private final float partitionNoiseFilter;
    private List<Weights> weights;
    private boolean useCrossValidation;
    private Boolean preExecuteFilter;
    private Class<? extends ILogSplitter> logSplitter;
    private Set<String> filenames;

    //endregoin

    //region builder class
    public static class AdaptiveNoiseBenchmarkConfigurationBuilder {

        private float[] noiseThresholds;

        private List<Weights> weights;
        private float partitionNoiseFilter;
        private Boolean preExecuteFilter;
        private boolean useCrossValidation;
        private Class<? extends ILogSplitter> logSplitter;
        private Logs logs;

        public AdaptiveNoiseBenchmarkConfigurationBuilder(){
            this.weights = new ArrayList<>();
        }

        public AdaptiveNoiseBenchmarkConfigurationBuilder setNoiseThresholds(NoiseThreshold noiseThresholds){
            this.noiseThresholds = noiseThresholds.getThresholds();
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

        public AdaptiveNoiseBenchmarkConfigurationBuilder addLogs(Logs logs){
            this.logs = logs;
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

        public AdaptiveNoiseBenchmarkConfigurationBuilder setPreExecuteFilter(boolean preExecuteFilter) {
            this.preExecuteFilter = preExecuteFilter;
            return this;
        }

        public AdaptiveNoiseBenchmarkConfigurationBuilder setLogSplitter(Class<? extends ILogSplitter> logSplitter) {
            this.logSplitter = logSplitter;
            return this;
        }

        public float[] getNoiseThresholds() {
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

        public Boolean isPreExecuteFilter() {
            return preExecuteFilter;
        }

        public Class<? extends ILogSplitter> getLogSplitter() {
            return logSplitter;
        }

        public Logs getLogs(){
            return this.logs;
        }
    }
    //endregion

    private AdaptiveNoiseBenchmarkConfiguration(AdaptiveNoiseBenchmarkConfigurationBuilder builder){
        this.noiseThresholds = builder.getNoiseThresholds();
        this.weights = builder.getWeights();
        this.useCrossValidation = builder.getUseCrossValidation();
        this.partitionNoiseFilter = builder.getPartitionNoiseFilter();
        this.preExecuteFilter = builder.isPreExecuteFilter();
        this.logSplitter = builder.getLogSplitter();
        this.filenames = builder.getLogs().getFiles();
    }

    public float[] getNoiseThresholds() {
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

    public boolean isPreExecuteFilter() {
        return preExecuteFilter;
    }

    public Class<? extends ILogSplitter> getLogSplitterClass() {
        return logSplitter;
    }

    public Set<String> getFilenames() {
        return filenames;
    }

    /*
    public AdaptiveNoiseMiner getMiner(String filename) throws Exception {
        return new AdaptiveNoiseMiner(filename, this);
    }
    */

    public AdaptiveNoiseConfiguration getAdaptiveNoiseConfiguration(Weights weights, boolean preExecuteFilter){
        return AdaptiveNoiseConfiguration.getBuilder()
                .setNoiseThresholds(this.getNoiseThresholds())
                .setPartitionNoiseFilter(this.partitionNoiseFilter)
                .setPreExecuteFilter(preExecuteFilter)
                .setLogSplitter(this.logSplitter)
                .setWeights(weights)
                .build();
    }
}
