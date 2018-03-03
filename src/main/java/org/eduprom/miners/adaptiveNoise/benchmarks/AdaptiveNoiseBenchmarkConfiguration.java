package org.eduprom.miners.adaptiveNoise.benchmarks;

import org.eduprom.benchmarks.configuration.Logs;
import org.eduprom.benchmarks.configuration.NoiseThreshold;
import org.eduprom.benchmarks.configuration.Weights;
import org.eduprom.exceptions.MiningException;
import org.eduprom.miners.adaptiveNoise.configuration.AdaptiveNoiseConfiguration;
import org.eduprom.partitioning.ILogSplitter;
import org.eduprom.partitioning.InductiveCutSplitting;
import org.eduprom.partitioning.trunk.InductiveLogSplitting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AdaptiveNoiseBenchmarkConfiguration {

    //region private members

    private float[] noiseThresholds;
    private final float partitionNoiseFilter;
    private List<Weights> weights;
    private boolean useCrossValidation;
    private Class<? extends ILogSplitter> logSplitter;
    private Set<String> filenames;

    //endregoin

    //region builder class
    public static class AdaptiveNoiseBenchmarkConfigurationBuilder {

        private final Class<? extends ILogSplitter> DEFAULT_LOG_SPLITTER =  InductiveCutSplitting.class; //AdaMiner.class;
        private final Weights DEFAULT_WEIGHTS = Weights.getUniform();
        private final NoiseThreshold DEFAULT_NOISE_THRESHOLDS = NoiseThreshold.uniform(0.2f);

        private float[] noiseThresholds;

        private List<Weights> weights;
        private float partitionNoiseFilter;
        private boolean useCrossValidation;
        private Class<? extends ILogSplitter> logSplitter;
        private Logs logs;

        public AdaptiveNoiseBenchmarkConfigurationBuilder() throws MiningException {
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

        public AdaptiveNoiseBenchmarkConfiguration build() {
            if (this.weights.isEmpty()){
                addWeights(DEFAULT_WEIGHTS);
            }
            if (this.noiseThresholds == null || this.noiseThresholds.length == 0) {
                setNoiseThresholds(DEFAULT_NOISE_THRESHOLDS);
            }
            if (this.logSplitter == null){
                setLogSplitter(DEFAULT_LOG_SPLITTER);
            }

            return new AdaptiveNoiseBenchmarkConfiguration(this);
        }

        public float getPartitionNoiseFilter() {
            return partitionNoiseFilter;
        }

        public List<Weights> getWeights(){
            return this.weights;
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
        this.weights =  builder.getWeights();
        Collections.shuffle(this.weights);
        this.useCrossValidation = builder.getUseCrossValidation();
        this.partitionNoiseFilter = builder.getPartitionNoiseFilter();
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

    public static AdaptiveNoiseBenchmarkConfigurationBuilder getBuilder() throws MiningException {
        return new AdaptiveNoiseBenchmarkConfigurationBuilder();
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
