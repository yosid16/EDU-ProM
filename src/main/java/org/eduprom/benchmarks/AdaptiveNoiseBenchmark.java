
package org.eduprom.benchmarks;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.apache.commons.lang.StringUtils;
import org.deckfour.xes.model.XLog;
import org.eduprom.entities.CrossValidationPartition;
import org.eduprom.exceptions.ConformanceCheckException;
import org.eduprom.exceptions.ExportFailedException;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner;
import org.eduprom.miners.adaptiveNoise.ConformanceInfo;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.miners.adaptiveNoise.TreeChanges;
import org.eduprom.miners.adaptiveNoise.configuration.AdaptiveNoiseConfiguration;
import org.eduprom.utils.LogHelper;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdaptiveNoiseBenchmark implements IBenchmark {

    static final String FITNESS_KEY = "Move-Model Fitness";
    private LogHelper logHelper;
    private final AdaptiveNoiseConfiguration adaptiveNoiseConfiguration;
    private List<String> filenames;

    private ConformanceInfo getNewConformanceInfo(){
        return new ConformanceInfo(adaptiveNoiseConfiguration.getFitnessWeight(),
                adaptiveNoiseConfiguration.getPrecisionWeight(),
                adaptiveNoiseConfiguration.getGeneralizationWeight());

    }

    public AdaptiveNoiseBenchmark(List<String> filenames, AdaptiveNoiseConfiguration adaptiveNoiseConfiguration){
        this.adaptiveNoiseConfiguration = adaptiveNoiseConfiguration;
        this.filenames = filenames;
        this.logHelper = new LogHelper();
    }

    @Override
    public IBenchmarkableMiner getSource(String filename) throws Exception {

        return new AdaptiveNoiseMiner(filename, adaptiveNoiseConfiguration);
    }

    @Override
    public List<IBenchmarkableMiner> getTargets(String filename) throws LogFileNotFoundException {
        List<IBenchmarkableMiner> benchmarkableMiners = new ArrayList<>();
        for(float noiseThreshold: this.adaptiveNoiseConfiguration.getNoiseThresholds()){
            benchmarkableMiners.add(new NoiseInductiveMiner(filename, noiseThreshold));
        }

        return benchmarkableMiners;
    }


    public void run() throws Exception {
        for(String filename: filenames){

            //split the log to training and test sets.
            XLog log = logHelper.read(filename);

            List<CrossValidationPartition> crossValidationPartitions =  this.logHelper.crossValidationSplit(log, 10);
            CrossValidationPartition testPartition = crossValidationPartitions.stream().findAny().get();
            XLog testLog = testPartition.getLog();
            XLog trainingLog = CrossValidationPartition.Bind(crossValidationPartitions.stream().filter(x -> testPartition != x)
                    .collect(Collectors.toList())).getLog();

            //get the miners
            IBenchmarkableMiner source = getSource(filename);
            List<IBenchmarkableMiner> targets = getTargets(filename);

            //assign the training set
            source.setLog(trainingLog);
            targets.forEach(x->x.setLog(trainingLog));

            //mine the models
            source.mine();
            evaluate(source, trainingLog, testLog);


            IBenchmarkableMiner bestBaseline = null;
            for(IBenchmarkableMiner miner: targets){
                miner.mine();
                evaluate(miner, trainingLog, testLog);
                if (bestBaseline == null || bestBaseline.getConformanceInfo().getPsi() <= miner.getConformanceInfo().getPsi()){
                    bestBaseline = miner;
                }
            }

            sendResult(source, bestBaseline, filename);
        }
    }

    private void sendResult(IBenchmarkableMiner source, IBenchmarkableMiner bestBaseline, String filename) throws ExportFailedException {
        try{
            String path = String.format("./Output/%s.csv", this.getName());

            boolean appendSchema = true;
            if (new File(path).isFile()){
                FileReader fileReader = new FileReader(path);
                CSVReader reader = new CSVReader(fileReader);

                if (reader.readAll().size() > 0){
                    appendSchema = false;
                }
                fileReader.close();
                reader.close();
            }
            AdaptiveNoiseMiner adaptiveNoiseMiner = (AdaptiveNoiseMiner) source;
            NoiseInductiveMiner inductiveMiner = (NoiseInductiveMiner) bestBaseline;
            TreeChanges bestModel = adaptiveNoiseMiner.getBestModel();
            int optionsScanned = adaptiveNoiseMiner.getChanges().size();



            CSVWriter csvWriter = new CSVWriter(new FileWriter(path, true));
            String[] schema = new String[]
                    {
                            "filename",
                            "duration",
                            "options_scanned",
                            "noise_thresholds",
                            "best_psi",
                            "best_fitness",
                            "best_precision",
                            "best_generalization",
                            "best_bits_removed",
                            "num_sublogs_changed",
                            "baseline_noise",
                            "baseline_psi",
                            "baseline_fitness",
                            "baseline_precision",
                            "baseline_generalization",
                            "baseline_bits_removed",
                            "psi_improvement"
                    };

            String[] data = new String[] {
                    filename,
                    String.valueOf(source.getElapsedMiliseconds()),
                    String.valueOf(optionsScanned),
                    org.apache.commons.lang3.StringUtils.join(this.adaptiveNoiseConfiguration.getNoiseThresholds(), ',') ,
                    String.valueOf(bestModel.getConformanceInfo().getPsi()),
                    String.valueOf(bestModel.getConformanceInfo().getFitness()),
                    String.valueOf(bestModel.getConformanceInfo().getPrecision()),
                    String.valueOf(bestModel.getConformanceInfo().getGeneralization()),
                    String.valueOf(bestModel.getBitsRemoved()),
                    String.valueOf(bestModel.getNumberOfChanges()),
                    String.valueOf(inductiveMiner.getNoiseThreshold()),
                    String.valueOf(inductiveMiner.getConformanceInfo().getPsi()),
                    String.valueOf(inductiveMiner.getConformanceInfo().getFitness()),
                    String.valueOf(inductiveMiner.getConformanceInfo().getPrecision()),
                    String.valueOf(inductiveMiner.getConformanceInfo().getGeneralization()),
                    String.valueOf(inductiveMiner.getResult().getFilterResult().getBitsRemoved()),
                    String.valueOf(bestModel.getConformanceInfo().getPsi() - inductiveMiner.getConformanceInfo().getPsi())};

            if (appendSchema){
                csvWriter.writeNext(schema);
            }
            csvWriter.writeNext(data);
            csvWriter.close();
        }
        catch (Exception e){
            throw new ExportFailedException(e);
        }
    }


    private void evaluate(IBenchmarkableMiner miner, XLog trainingLog, XLog testLog) throws ConformanceCheckException {
        ProcessTree2Petrinet.PetrinetWithMarkings model = miner.getModel();
        PNRepResult alignment = miner.getHelper().getAlignment(trainingLog,
                model.petrinet, model.initialMarking, model.finalMarking);

        ConformanceInfo info = getNewConformanceInfo();
        double fitness = Double.parseDouble(alignment.getInfo().get(FITNESS_KEY).toString());
        info.setFitness(fitness);

        double precision = miner.getHelper().getPrecision(trainingLog, model.petrinet, alignment, model.initialMarking, model.finalMarking);
        info.setPrecision(precision);

        PNRepResult testAlignment = miner.getHelper().getAlignment(testLog, model.petrinet, model.initialMarking, model.finalMarking);
        double generalization = Double.parseDouble(testAlignment.getInfo().get(FITNESS_KEY).toString());
        info.setGeneralization(generalization);
        miner.setConformanceInfo(info);
    }

    public String getName(){
        return "AdaptiveNoise";
    }
}
