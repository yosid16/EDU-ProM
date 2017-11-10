
package org.eduprom.miners.adaptiveNoise.benchmarks;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.deckfour.xes.model.XLog;
import org.eduprom.benchmarks.IBenchmark;
import org.eduprom.benchmarks.IBenchmarkableMiner;
import org.eduprom.benchmarks.Weights;
import org.eduprom.exceptions.ConformanceCheckException;
import org.eduprom.exceptions.ExportFailedException;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.miners.AbstractMiner;
import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner;
import org.eduprom.miners.adaptiveNoise.ConformanceInfo;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.miners.adaptiveNoise.TreeChanges;
import org.eduprom.utils.LogHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.processmining.plugins.petrinet.replayresult.PNRepResult.TRACEFITNESS;

public class AdaptiveNoiseBenchmarkDfci implements IBenchmark {

    protected static final Logger logger = Logger.getLogger(AbstractMiner.class.getName());
    static final String FITNESS_KEY = TRACEFITNESS;
    private final String testFile;
    private final String trainFile;
    private LogHelper logHelper;
    private final AdaptiveNoiseBenchmarkConfiguration adaptiveNoiseConfiguration;
    private List<String> filenames;
    private String path;
    //private int testSize;

    /*
    private Set<String> getTraces(XLog log){
        return log.stream().map(x->x.stream().map(y->y.getAttributes().get("concept:name").toString())
                .collect(Collectors.joining("->"))).collect(Collectors.toSet());
    }*/
    private ConformanceInfo getNewConformanceInfo(Weights weights){
        return new ConformanceInfo(weights.getFitnessWeight(),
                weights.getPrecisionWeight(),
                weights.getGeneralizationWeight());

    }

    private void evaluate(IBenchmarkableMiner miner, XLog trainingLog, XLog testLog, Weights weights) throws ConformanceCheckException {
        ProcessTree2Petrinet.PetrinetWithMarkings model = miner.getModel();
        PNRepResult alignment = miner.getHelper().getAlignment(trainingLog,
                model.petrinet, model.initialMarking, model.finalMarking);

        ConformanceInfo info = getNewConformanceInfo(weights);
        double fitness = Double.parseDouble(alignment.getInfo().get(FITNESS_KEY).toString());
        info.setFitness(fitness);

        double precision = miner.getHelper().getPrecision(trainingLog, model.petrinet, alignment, model.initialMarking, model.finalMarking);
        info.setPrecision(precision);

        PNRepResult testAlignment = miner.getHelper().getAlignment(testLog, model.petrinet, model.initialMarking, model.finalMarking);
        double generalization = Double.parseDouble(testAlignment.getInfo().get(FITNESS_KEY).toString());
        info.setGeneralization(generalization);
        miner.setConformanceInfo(info);
        //logger.info(String.format("miner: %s, conformance: %s", miner.getName(), miner.getConformanceInfo()));
    }

    public AdaptiveNoiseBenchmarkDfci(String trainFile, String testFile, AdaptiveNoiseBenchmarkConfiguration adaptiveNoiseBenchmarkConfiguration){
        this.adaptiveNoiseConfiguration = adaptiveNoiseBenchmarkConfiguration;
        this.filenames = filenames;
        this.logHelper = new LogHelper();
        //this.testSize = testSize;
        String format = "./Output/%s-%s.csv";
        this.path = String.format(format, this.getName(),
                new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date()));
        this.trainFile = trainFile;
        this.testFile = testFile;
    }

    @Override
    public List<AdaptiveNoiseMiner> getSources(String filename) throws Exception {
        List<AdaptiveNoiseMiner> miners = new ArrayList<>();
        for(Weights weights : this.adaptiveNoiseConfiguration.getWeights()){
            miners.add(new AdaptiveNoiseMiner(filename, this.adaptiveNoiseConfiguration.getAdaptiveNoiseConfiguration(weights)));
        }

        return miners;
    }

    @Override
    public List<NoiseInductiveMiner> getTargets(String filename) throws LogFileNotFoundException {
        List<NoiseInductiveMiner> benchmarkableMiners = new ArrayList<>();
        for(float noiseThreshold: this.adaptiveNoiseConfiguration.getNoiseThresholds()){
            benchmarkableMiners.add(new NoiseInductiveMiner(filename, noiseThreshold, false));
        }

        return benchmarkableMiners;
    }


    public void run() throws Exception {

        //split the log to training and test sets.

        XLog testLog = logHelper.read(testFile);
        XLog trainingLog = logHelper.read(trainFile);

        for(IBenchmarkableMiner source: getSources(trainFile)){
            AdaptiveNoiseMiner adaptiveNoiseMiner = (AdaptiveNoiseMiner) source;
            Weights weights = adaptiveNoiseMiner.getConfiguration().getWeights();
            //get the miners
            List<NoiseInductiveMiner> targets = getTargets(trainFile);

            //assign the training set
            source.setLog(trainingLog);
            targets.forEach(x->x.setLog(trainingLog));

            //mine the models
            source.mine();
            source.export();
            evaluate(source, trainingLog, testLog, weights);


            IBenchmarkableMiner bestBaseline = null;
            for(IBenchmarkableMiner miner: targets){
                miner.mine();
                evaluate(miner, trainingLog, testLog, weights);
                if (bestBaseline == null || bestBaseline.getConformanceInfo().getPsi() <= miner.getConformanceInfo().getPsi()){
                    bestBaseline = miner;
                }
            }
            bestBaseline.export();

            logger.log(Level.INFO, String.format("BEST BASELINE: noise:%f  conformance: %s",
                    ((NoiseInductiveMiner)bestBaseline).getNoiseThreshold(),bestBaseline.getConformanceInfo().toString()));
            sendResult(source, bestBaseline, trainFile);

        }
    }

    private void sendResult(IBenchmarkableMiner source, IBenchmarkableMiner bestBaseline, String filename) throws ExportFailedException {
        try{
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
                            "group",
                            "filename",
                            "number_of_partitions",
                            "duration",
                            "options_scanned",
                            "noise_thresholds",
                            "fitness_weight",
                            "precision_weight",
                            "generalization_weight",
                            "best_psi",
                            "best_fitness",
                            "best_precision",
                            "best_generalization",
                            "num_sublogs_changed",
                            "baseline_noise",
                            "baseline_psi",
                            "baseline_fitness",
                            "baseline_precision",
                            "baseline_generalization",
                            "psi_improvement"
                    };
            String[] data = new String[] {
                    filename.contains("2017") ? "BPM-2017" : "BPM-2016",
                    filename.replace("EventLogs\\contest_2017\\log", "BPM-2017-Log")
                            .replace("EventLogs\\contest_dataset\\training_log_","BPM-2016-Log")
                            .replace(".xes", ""),
                    String.valueOf(bestModel.getPratitioning().getPartitions().size()),
                    String.valueOf(source.getElapsedMiliseconds()),
                    String.valueOf(optionsScanned),
                    org.apache.commons.lang3.StringUtils.join(this.adaptiveNoiseConfiguration.getNoiseThresholds(), ',') ,
                    String.valueOf(adaptiveNoiseMiner.getConformanceInfo().getFitnessWeight()),
                    String.valueOf(adaptiveNoiseMiner.getConformanceInfo().getPrecisionWeight()),
                    String.valueOf(adaptiveNoiseMiner.getConformanceInfo().getGeneralizationWeight()),
                    String.valueOf(bestModel.getConformanceInfo().getPsi()),
                    String.valueOf(bestModel.getConformanceInfo().getFitness()),
                    String.valueOf(bestModel.getConformanceInfo().getPrecision()),
                    String.valueOf(bestModel.getConformanceInfo().getGeneralization()),
                    String.valueOf(bestModel.getNumberOfChanges()),
                    String.valueOf(inductiveMiner.getNoiseThreshold()),
                    String.valueOf(inductiveMiner.getConformanceInfo().getPsi()),
                    String.valueOf(inductiveMiner.getConformanceInfo().getFitness()),
                    String.valueOf(inductiveMiner.getConformanceInfo().getPrecision()),
                    String.valueOf(inductiveMiner.getConformanceInfo().getGeneralization()),
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

    public String getName(){
        return "AdaptiveNoise";
    }
}
