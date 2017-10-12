package org.eduprom.miners.adaptiveNoise;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import javafx.util.Pair;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.eduprom.entities.CrossValidationPartition;
import org.eduprom.exceptions.ExportFailedException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.exceptions.ProcessTreeConversionException;
import org.eduprom.miners.AbstractPetrinetMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.MiningResult;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.miners.adaptiveNoise.conformance.IConformanceContext;
import org.eduprom.miners.adaptiveNoise.conformance.IConformanceObject;
import org.eduprom.partitioning.ILogSplitter;
import org.eduprom.partitioning.InductiveLogSplitting;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.modelrepair.plugins.align.PNLogReplayer;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.math3.*;

public class RecursiveScan extends AbstractPetrinetMiner implements IConformanceContext {

    private Float[] noiseThresholds;
    private List<NoiseInductiveMiner> miners;

    private final NoiseInductiveMiner partitionMiner = NoiseInductiveMiner.WithNoiseThreshold(filename, 0f);
    private double precisionWeight;
    private double fitnessWeight;
    private Double generalizationWeight;

    //region private methods

    /*
    private void modifyPsi() throws MiningException {
        ProcessTree2Petrinet.PetrinetWithMarkings res = null;
        try {
            res = PetrinetHelper.ConvertToPetrinet(this.getProcessTree());
        } catch (ProcessTreeConversionException e) {
            throw new MiningException(e);
        }

        //String path = String.format("./Output/%s_%s_%s" , getName(),
        //        FilenameUtils.removeExtension(Paths.get(filename).getFileName().toString()), changes.id.toString());
        //petrinetHelper.export(res.petrinet, path);

        PNRepResult alignment = petrinetHelper.getAlignment(log, res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        //this.petrinetHelper.printResults(alignment);
        this.info.setFitness(fitness);

        double precision = petrinetHelper.getPrecision(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
        info.setPrecision(precision);

        //double generalization = petrinetHelper.getGeneralization(log, res);
        //info.setGeneralization(generalization);
    }*/


    private void modifyPsiWithGen(IConformanceObject object) throws MiningException {
        object.setConformanceInfo(new ConformanceInfo(fitnessWeight, precisionWeight, generalizationWeight));
        MiningResult result = object.getMiner().mineProcessTree(object.getLog(), object.getId());
        object.setMiningResult(result);
        ProcessTree2Petrinet.PetrinetWithMarkings res = null;
        try {
            res = PetrinetHelper.ConvertToPetrinet(result.getProcessTree());
        } catch (ProcessTreeConversionException e) {
            throw new MiningException(e);
        }

        //String path = String.format("./Output/%s_%s_%s" , getName(),
        //        FilenameUtils.removeExtension(Paths.get(filename).getFileName().toString()), changes.id.toString());
        //petrinetHelper.export(res.petrinet, path);

        PNRepResult alignment = petrinetHelper.getAlignment(object.getLog(), res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        //this.petrinetHelper.printResults(alignment);
        object.getConformanceInfo().setFitness(fitness);

        AlignmentPrecGenRes alignmentPrecGenRes = petrinetHelper.getConformance(object.getLog(), res.petrinet, alignment, res.initialMarking, res.finalMarking);
        object.getConformanceInfo().setPrecision(alignmentPrecGenRes.getPrecision());
        object.getConformanceInfo().setGeneralization(alignmentPrecGenRes.getGeneralization());

        //double generalization = petrinetHelper.getGeneralization(log, res);
        //object.getConformanceInfo().setGeneralization(1.0);
    }


    private void modifyPsiNoGen(IConformanceObject object) throws MiningException {
        object.setConformanceInfo(new ConformanceInfo(fitnessWeight, precisionWeight, generalizationWeight));
        MiningResult result = object.getMiner().mineProcessTree(object.getLog(), object.getId());
        object.setMiningResult(result);
        ProcessTree2Petrinet.PetrinetWithMarkings res = null;
        try {
            res = PetrinetHelper.ConvertToPetrinet(result.getProcessTree());
        } catch (ProcessTreeConversionException e) {
            throw new MiningException(e);
        }

        //String path = String.format("./Output/%s_%s_%s" , getName(),
        //        FilenameUtils.removeExtension(Paths.get(filename).getFileName().toString()), changes.id.toString());
        //petrinetHelper.export(res.petrinet, path);

        PNRepResult alignment = petrinetHelper.getAlignment(object.getLog(), res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        //this.petrinetHelper.printResults(alignment);
        object.getConformanceInfo().setFitness(fitness);

        double precision = petrinetHelper.getPrecision(object.getLog(), res.petrinet, alignment, res.initialMarking, res.finalMarking);
        object.getConformanceInfo().setPrecision(precision);

        //double generalization = petrinetHelper.getGeneralization(log, res);
        object.getConformanceInfo().setGeneralization(1.0);
    }

    private void modifyPsiCrossValidation(IConformanceObject object) throws MiningException {
        Collections.shuffle(object.getLog());
        int k = object.getLog().size() > 10 ? 10 : 2;
        List<CrossValidationPartition> partitions = Lists.partition(object.getLog(), object.getLog().size() / 10)
                .stream().map(x -> new CrossValidationPartition(x, object.getLog().getAttributes())).collect(Collectors.toList());

        List<ConformanceInfo> values = new ArrayList<ConformanceInfo>();
        for(CrossValidationPartition testTraces: partitions){
            List<XTrace> trainTraces = partitions.stream()
                    .filter(x -> x != testTraces)
                    .flatMap(x -> x.getLog().stream()).collect(Collectors.toList());
            XLog trainLog = new XLogImpl(object.getLog().getAttributes());
            trainLog.addAll(trainTraces);

            XLog testLog = new XLogImpl(object.getLog().getAttributes());
            testLog.addAll(testTraces.getLog());

            MiningResult result = object.getMiner().mineProcessTree(trainLog);
            ProcessTree2Petrinet.PetrinetWithMarkings res = PetrinetHelper.ConvertToPetrinet(result.getProcessTree());


            ConformanceInfo candidate = new ConformanceInfo(fitnessWeight,
                    precisionWeight, generalizationWeight);

            PNRepResult alignment = petrinetHelper.getAlignment(trainLog, res.petrinet, res.initialMarking, res.finalMarking);
            double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
            candidate.setFitness(fitness);

            double precision = petrinetHelper.getPrecision(trainLog, res.petrinet, alignment, res.initialMarking, res.finalMarking);
            candidate.setPrecision(precision);

            PNRepResult testAlignment = petrinetHelper.getAlignment(testLog, res.petrinet, res.initialMarking, res.finalMarking);
            double generalization = Double.parseDouble(testAlignment.getInfo().get("Move-Model Fitness").toString());
            candidate.setGeneralization(generalization);
            values.add(candidate);
        }

        MiningResult result = object.getMiner().mineProcessTree(object.getLog());
        object.setMiningResult(result);
        ConformanceInfo conformanceInfo = new ConformanceInfo(fitnessWeight, precisionWeight, generalizationWeight);
        conformanceInfo.setFitness(values.stream().mapToDouble(x->x.getFitness()).sum() / values.size());
        conformanceInfo.setPrecision(values.stream().mapToDouble(x->x.getPrecision()).sum() / values.size());
        conformanceInfo.setGeneralization(values.stream().mapToDouble(x->x.getGeneralization()).sum() / values.size());
        object.setConformanceInfo(conformanceInfo);
    }


    private void modifyPsiRollup(TreeChanges changes, XLog trainLog, XLog testLog) throws MiningException {
        //Collections.shuffle(log);
        //List<CrossValidationPartition> partitions = logHelper.crossValidationSplit(this.log, 10);


        ProcessTree2Petrinet.PetrinetWithMarkings res = null;
        try {
            res = PetrinetHelper.ConvertToPetrinet(changes.getModifiedProcessTree());
        } catch (ProcessTreeConversionException e) {
            throw new MiningException(e);
        }

        //String path = String.format("./Output/%s_%s_%s" , getName(),
        //        FilenameUtils.removeExtension(Paths.get(filename).getFileName().toString()), changes.id.toString());
        //petrinetHelper.export(res.petrinet, path);

        PNRepResult alignment = petrinetHelper.getAlignment(trainLog, res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        //this.petrinetHelper.printResults(alignment);
        changes.getConformanceInfo().setFitness(fitness);

        double precision = petrinetHelper.getPrecision(trainLog, res.petrinet, alignment, res.initialMarking, res.finalMarking);
        changes.getConformanceInfo().setPrecision(precision);


        PNRepResult testAlignment = petrinetHelper.getAlignment(testLog, res.petrinet, res.initialMarking, res.finalMarking);
        double generalization = Double.parseDouble(testAlignment.getInfo().get("Move-Model Fitness").toString());
        changes.getConformanceInfo().setGeneralization(generalization);

        //AlignmentPrecGenRes alignmentPrecGenRes = petrinetHelper.getConformance(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
        //changes.getConformanceInfo().setGeneralization(alignmentPrecGenRes.getGeneralization());
        //changes.getConformanceInfo().setPrecision(alignmentPrecGenRes.getPrecision());

        /*
        Partitioning.PartitionInfo rootInfo = changes.getPratitioning()
                .getPartitions().values().stream()
                .filter(x->x.isRoot()).findAny().get();
        int rootBits = rootInfo.getBits();

        Map<ConformanceInfo, Double> conformanceInfos = new ConcurrentHashMap<>();
        //changes.getUnchangedPartitions().values().stream()
        for(Map.Entry<UUID, Partitioning.PartitionInfo> entry : changes.getUnchangedPartitions().entrySet()){
            double weight = (double)entry.getValue().getBits() / rootBits;
            conformanceInfos.put(entry.getValue().getConformanceInfo(), weight);
        }

        for(Map.Entry<UUID, Change> entry : changes.getChangesMap().entrySet()){
            double weight = (double)entry.getValue().getBits() / rootBits;
            conformanceInfos.put(entry.getValue().getConformanceInfo(), weight);
        }


        Double min = conformanceInfos.values().stream().mapToDouble(x->x).min().getAsDouble();
        Double max = conformanceInfos.values().stream().mapToDouble(x->x).max().getAsDouble();
        Double sum = conformanceInfos.values().stream().mapToDouble(x->x).sum();
        conformanceInfos.entrySet().forEach(x -> conformanceInfos.put(x.getKey(), x.getValue() /sum ));

        ConformanceInfo info = changes.getConformanceInfo();
        info.setFitness(conformanceInfos.entrySet().stream().mapToDouble(x -> x.getKey().getFitness() * x.getValue()).sum());
        info.setPrecision(conformanceInfos.entrySet().stream().mapToDouble(x -> x.getKey().getPrecision() * x.getValue()).sum());
        info.setGeneralization(conformanceInfos.entrySet().stream().mapToDouble(x -> x.getKey().getGeneralization() * x.getValue()).sum());
        //info.setFitness(conformanceInfos.stream().mapToDouble(x -> x.getKey().getFitness() * ((x.getValue() - min) / (max - min))).sum());
        //info.setPrecision(conformanceInfos.stream().mapToDouble(x -> x.getKey().getPrecision() * ((x.getValue() - min) / (max - min))).sum());
        //info.setGeneralization(conformanceInfos.stream().mapToDouble(x -> x.getKey().getGeneralization() * ((x.getValue() - min) / (max - min))).sum());
        */

    }

    private Partitioning splitLog(XLog trainLog) throws MiningException {

        ILogSplitter logSplitter = new InductiveLogSplitting(this);
        Partitioning pratitioning = logSplitter.split(trainLog);

        for(Partitioning.PartitionInfo partitionInfo : pratitioning.getPartitions().values()) {
            modifyPsiCrossValidation(partitionInfo);

            //logger.info(String.format("Conformance on log split: %s", partitionInfo.getConformanceInfo().toString()));
        }
        return pratitioning;
    }

    private Set<TreeChanges> generatePossibleTreeChanges(Partitioning pratitioning, Set<Change> changeOptions) throws MiningException {

        HashMap<TreeChangesSet, TreeChanges> allChanges = new HashMap<>();
        Queue<TreeChanges> current = new LinkedList<>();
        TreeChanges baselineChange = new TreeChanges(pratitioning, fitnessWeight, precisionWeight, generalizationWeight);
        current.offer(baselineChange);
        allChanges.put(baselineChange.getChanges(), baselineChange);

        int scanned = 0;
        while (!current.isEmpty()) {
            TreeChanges currentChange = current.poll();

            List<Change> changes = currentChange.getApplicableChanges(changeOptions)
                    .filter(x -> !allChanges.containsKey(currentChange.getChanges().toTreeChangesSet().add(x)))
                    .collect(Collectors.toList());

            //logger.info(String.format("changes size: %d", changes.size()));

            for(Change change : changes) {
                TreeChanges newSln = currentChange.ToTreeChanges();
                if (newSln.Add(change)) { // replacement is feasible
                    if (!allChanges.containsKey(newSln.getChanges())){
                        allChanges.put(newSln.getChanges(), newSln);
                        current.offer(newSln);
                        scanned++;
                    }
                }
            }


            //logger.info(String.format("stack size: %d, scanned: %d", current.size(), scanned));
        }


        //allChanges.removeIf(x -> x.getNumberOfChanges() == 0);
        return allChanges.values().stream().collect(Collectors.toSet());
    }

    private Set<TreeChanges> generatePossibleTreeChanges2(Partitioning pratitioning, Set<Change> changeOptions) throws MiningException {

        Set<Set<Change>> changesSet = Sets.newConcurrentHashSet(changeOptions.stream()
                .map(x -> {
                    HashSet<Change> s = new HashSet<Change>();
                    s.add(x);
                    return s;
                }).collect(Collectors.toList()));

        for(Change change1 : changeOptions) {
            for (Change change2 : changeOptions) {
                if (!change1.getPartitionInfo().isRalated(change2.getPartitionInfo())){
                    changesSet.stream().filter(x->x.contains(change1))
                            .forEach(x->
                            {
                                if (x.stream().allMatch(y -> !y.getPartitionInfo().isRalated(change2.getPartitionInfo()))){
                                    Set<Change> newSln = x.stream().collect(Collectors.toSet());
                                    newSln.add(change2);
                                    changesSet.add(newSln);
                                }
                            });

                    changesSet.stream().filter(x->x.contains(change2))
                            .forEach(x->
                            {
                                if (x.stream().allMatch(y -> !y.getPartitionInfo().isRalated(change1.getPartitionInfo()))){
                                    Set<Change> newSln = x.stream().collect(Collectors.toSet());
                                    newSln.add(change1);
                                    changesSet.add(newSln);
                                }
                            });
                }
            }
        }

        Set<Set<Change>> possibleChanges = new HashSet<>();
        for (Set<Change> changes: changesSet){
            possibleChanges.addAll(Sets.powerSet(changes));
        }

        HashMap<TreeChangesSet, TreeChanges> allChanges = new HashMap<>();
        TreeChanges baselineChange = new TreeChanges(pratitioning, fitnessWeight, precisionWeight, generalizationWeight);
        allChanges.put(baselineChange.getChanges(), baselineChange);
        int scanned = 0;
        for (Set<Change> changes: possibleChanges){
            TreeChanges newSln = baselineChange.ToTreeChanges();
            boolean success = true;
            for(Change change : changes) {
                if (!newSln.Add(change)){
                    success = false;
                    break;
                }
            }
            if (success){
                allChanges.put(newSln.getChanges(), newSln);
            }
            else {
                //throw new MiningException("why?!");
            }
        }

        return allChanges.values().stream().collect(Collectors.toSet());
    }


    public Set<Change> getOptions(Partitioning pratitioning, List<NoiseInductiveMiner> miners) throws MiningException {
        Set<Change> changes = pratitioning.getPartitions().entrySet().stream()
                .filter(x -> x.getValue().getConformanceInfo().getPsi() < 1.0)
                .flatMap(x-> miners.stream()
                        .map(miner -> new Change(x.getValue(), x.getValue().getLog(), miner, this)))
                .collect(Collectors.toSet());

        for(Change change : changes){
            //logger.info(String.format("started change: %s", change));
            modifyPsiCrossValidation(change);
            //logger.info(String.format("Conformance on option: %s", change.getConformanceInfo().toString()));
        }

        //changes.stream().forEach(x -> x.discover());
        //changes.removeIf(x -> x.getBitsChanged() == 0);

        return changes;
    }

    private TreeChanges findOptimal(Set<TreeChanges> treeChanges, XLog trainLog, XLog testLog) throws MiningException {
        double bestPsi = 0;
        TreeChanges bestModel = null;
        HashSet<String> scanned = new HashSet<>();



        for(TreeChanges change : treeChanges) {
            String key = change.getModifiedProcessTree().toString();
            if (scanned.contains(key) && !change.isBaseline()){
                continue;
            }
            modifyPsiRollup(change, trainLog, testLog);
            //logger.info("OPTIONAL MODEL: " + change.toString());

            if (change.getConformanceInfo().getPsi() > bestPsi) {
                bestModel = change;
                bestPsi = change.getConformanceInfo().getPsi();
            }

            scanned.add(key);
        }

        return bestModel;
    }

    private TreeChanges findBaseline(Set<TreeChanges> treeChanges) throws MiningException {
        TreeChanges bestBaseline = null;
        for(TreeChanges change : treeChanges){
            if (!change.isBaseline()){
                continue;
            }

            logger.info("BASELINE MODEL: " + change.toString());
            if (bestBaseline == null || bestBaseline.getConformanceInfo().getPsi() < change.getConformanceInfo().getPsi()){
                bestBaseline = change;
            }
        }
        if(bestBaseline == null){
            throw new MiningException("Could not find a baseline");
        }

        return bestBaseline;
    }

    private void compare(Set<TreeChanges> treeChanges, TreeChanges bestModel, TreeChanges baselineModel, long elapsedSeconds) throws ExportFailedException {
        try{
            String path = String.format("./Output/RecursiveScan.csv", getOutputPath());
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

            int optionsScanned = treeChanges.size();

            OptionalDouble baselineNoise = baselineModel.getChanges().getChanges().stream().mapToDouble(x->x.getMiner().getNoiseThreshold()).findAny();

            CSVWriter csvWriter = new CSVWriter(new FileWriter(path, true));
            String[] schema = new String[] { "filename", "duration", "options_scanned", "noise_thresholds",
                    "best_psi", "best_fitness", "best_precision", "best_bits_removed", "num_sublogs_changed",
                    "baseline_noise",
                    "baseline_psi", "baseline_fitness", "baseline_precision", "baseline_bits_removed", "psi_improvement" };

            String[] data = new String[] {filename, String.valueOf(elapsedSeconds), String.valueOf(optionsScanned), StringUtils.join(this.noiseThresholds, ',') ,
                    String.valueOf(bestModel.getConformanceInfo().getPsi()), String.valueOf(bestModel.getConformanceInfo().getFitness()), String.valueOf(bestModel.getConformanceInfo().getPrecision()),
                    String.valueOf(bestModel.getBitsRemoved()),
                    String.valueOf(bestModel.getNumberOfChanges()),
                    String.valueOf(baselineNoise.orElse(0.0)), String.valueOf(baselineModel.getConformanceInfo().getPsi()), String.valueOf(baselineModel.getConformanceInfo().getFitness()), String.valueOf(baselineModel.getConformanceInfo().getPrecision()),
                    String.valueOf(baselineModel.getBitsRemoved()),
                    String.valueOf(bestModel.getConformanceInfo().getPsi() - baselineModel.getConformanceInfo().getPsi())};

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

    //endregion

    //region constructors

    public RecursiveScan(String filename, double fitnessWeight, double precisionWeight, double generalizationWeight, Float... noiseThresholds) throws Exception {
        super(filename);
        this.noiseThresholds = Stream.concat(Arrays.stream(noiseThresholds)
                ,Stream.of()).distinct().toArray(Float[]::new); //0.0f

        this.miners = NoiseInductiveMiner
                .WithNoiseThresholds(this.filename, this.noiseThresholds)
                .stream().collect(Collectors.toList());

        this.precisionWeight = precisionWeight;
        this.fitnessWeight = fitnessWeight;
        this.generalizationWeight = generalizationWeight;
    }

    //endregion

    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings minePetrinet() throws MiningException {

        List<CrossValidationPartition> crossValidationPartitions =  this.logHelper.crossValidationSplit(this.log, 10);
        CrossValidationPartition testPartition = crossValidationPartitions.stream().findAny().get();
        XLog testLog = testPartition.getLog();
        XLog trainLog = CrossValidationPartition.Bind(crossValidationPartitions.stream().filter(x -> testPartition != x)
                .collect(Collectors.toList())).getLog();


        //start measuring scan time
        Stopwatch stopwatch = Stopwatch.createStarted();

        logger.info(String.format("Fitness weight: %f, Precision weight: %f, generalization weight: %f",
                fitnessWeight, precisionWeight, generalizationWeight));

        //run algorithm
        Partitioning partitioning = splitLog(trainLog);
        logger.info(partitioning.toString());

        Set<Change> changeOptions = getOptions(partitioning, miners);

        logger.info(String.format("found %d possible changes to initial partitioning (#miners x #sublogs)",
                changeOptions.size()));

        Set<TreeChanges> changes = generatePossibleTreeChanges2(partitioning, changeOptions);
        logger.info(String.format("found %d potential solutions (all subsets of (miners x sublogs)). Total distinct trees: %d",
                changes.size(), changes.stream()
                        .map(x->x.getModifiedProcessTree().toString())
                        .collect(Collectors.toSet()).size()));

        TreeChanges bestModel = findOptimal(changes, trainLog, testLog);
        logger.info("OPTIMAL MODEL: " + bestModel.toString());

        TreeChanges baselineModel = findBaseline(changes);
        logger.info("BEST BASELINE MODEL: " + baselineModel.toString());

        //stop measuring time and compare results
        stopwatch.stop();
        compare(changes, bestModel, baselineModel, stopwatch.elapsed(TimeUnit.SECONDS));

        //return discoved process model
        return PetrinetHelper.ConvertToPetrinet(bestModel.getModifiedProcessTree());
    }

    @Override
    public NoiseInductiveMiner getPartitionMiner() {
        return this.partitionMiner;
    }
}


