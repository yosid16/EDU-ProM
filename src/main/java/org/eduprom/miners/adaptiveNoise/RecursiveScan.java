package org.eduprom.miners.adaptiveNoise;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Stopwatch;
import org.deckfour.xes.model.XLog;
import org.eduprom.exceptions.ExportFailedException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.exceptions.ProcessTreeConversionException;
import org.eduprom.miners.AbstractPetrinetMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.partitioning.ILogSplitter;
import org.eduprom.partitioning.InductiveLogSplitting;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecursiveScan extends AbstractPetrinetMiner {

    private Float[] noiseThresholds;
    private List<NoiseInductiveMiner> miners;

    private double precisionWeight;
    private double fitnessWeight;

    //region private methods

    private void modifyPsi(ConformanceInfo info, ProcessTree tree, XLog log) throws MiningException {
        ProcessTree2Petrinet.PetrinetWithMarkings res = null;
        try {
            res = PetrinetHelper.ConvertToPetrinet(tree);
        } catch (ProcessTreeConversionException e) {
            throw new MiningException(e);
        }

        //String path = String.format("./Output/%s_%s_%s" , getName(),
        //        FilenameUtils.removeExtension(Paths.get(filename).getFileName().toString()), changes.id.toString());
        //petrinetHelper.export(res.petrinet, path);

        PNRepResult alignment = petrinetHelper.getAlignment(log, res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        //this.petrinetHelper.printResults(alignment);
        info.setFitness(fitness);

        double precision = petrinetHelper.getPrecision(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
        info.setPrecision(precision);
    }

    private Partitioning splitLog() throws MiningException {

        ILogSplitter logSplitter = new InductiveLogSplitting();
        Partitioning pratitioning = logSplitter.split(this.log);

        NoiseInductiveMiner baselineMiner = miners.stream().filter(x->x.getNoiseThreshold() == 0).findAny().get();
        for(Partitioning.PartitionInfo partitionInfo : pratitioning.getPartitions().values()) {
            ConformanceInfo info = new ConformanceInfo(fitnessWeight, precisionWeight);
            ProcessTree pt = baselineMiner.mineProcessTree(partitionInfo.getLog());

            modifyPsi(info, pt, partitionInfo.getLog());

            partitionInfo.setProcessTree(pt);
            partitionInfo.setConformanceInfo(info);

        }
        return pratitioning;
    }

    private Set<TreeChanges> generatePossibleTreeChanges2(Partitioning pratitioning) throws MiningException {
        Set<Change> baseline = getOptions(pratitioning, miners);


        HashMap<TreeChangesSet, TreeChanges> allChanges = new HashMap<>();
        Queue<TreeChanges> current = new LinkedList<>();
        current.offer(new TreeChanges(pratitioning, fitnessWeight, precisionWeight));
        int scanned = 0;
        while (!current.isEmpty()) {
            TreeChanges currentChange = current.poll();

            List<Change> changes = currentChange.getApplicableChanges(baseline)
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


    public Set<Change> getOptions(Partitioning pratitioning, List<NoiseInductiveMiner> miners){
        return pratitioning.getPartitions().entrySet().stream()
                .filter(x -> x.getValue().getConformanceInfo().getPsi() < 0.8)
                .map(x->x.getKey())
                .flatMap(x-> miners.stream().map(miner -> new Change(x, miner)))
                .collect(Collectors.toSet());
    }

    private TreeChanges findOptimal(Set<TreeChanges> treeChanges) throws MiningException {
        double bestPsi = 0;
        TreeChanges bestModel = null;

        for(TreeChanges change : treeChanges) {
            modifyPsi(change.getConformanceInfo(), change.getModifiedProcessTree(), log);
            //logger.info("OPTIONAL MODEL: " + change.toString());

            if (change.getConformanceInfo().getPsi() > bestPsi){
                bestModel = change;
                bestPsi = change.getConformanceInfo().getPsi();
            }
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


            CSVWriter csvWriter = new CSVWriter(new FileWriter(path, true));
            String[] schema = new String[] { "filename", "duration", "options_scanned", "noise_thresholds",
                    "best_psi", "best_fitness", "best_precision", "best_sublogs_changed",
                    "baseline_psi", "baseline_fitness", "baseline_precision", "improvement" };

            String[] data = new String[] {filename, String.valueOf(elapsedSeconds), String.valueOf(optionsScanned), StringUtils.join(this.noiseThresholds, ',') ,
                    String.valueOf(bestModel.getConformanceInfo().getPsi()), String.valueOf(bestModel.getConformanceInfo().getFitness()), String.valueOf(bestModel.getConformanceInfo().getPrecision()), String.valueOf(bestModel.getNumberOfChanges()),
                    String.valueOf(baselineModel.getConformanceInfo().getPsi()), String.valueOf(baselineModel.getConformanceInfo().getFitness()), String.valueOf(baselineModel.getConformanceInfo().getPrecision()),
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

    public RecursiveScan(String filename, Float... noiseThresholds) throws Exception {
        super(filename);
        this.noiseThresholds = Stream.concat(Arrays.stream(noiseThresholds),
                Stream.of(0.0f)).distinct().toArray(Float[]::new);

        this.miners = NoiseInductiveMiner
                .WithNoiseThresholds(this.filename, this.noiseThresholds)
                .stream().collect(Collectors.toList());

        this.precisionWeight = 0.5;
        this.fitnessWeight = 0.5;
    }

    //endregion

    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings minePetrinet() throws MiningException {

        //start measuring scan time
        Stopwatch stopwatch = Stopwatch.createStarted();

        //run algorithm
        Partitioning pratitioning = splitLog();
        logger.info(pratitioning.toString());

        Set<TreeChanges> changes = generatePossibleTreeChanges2(pratitioning);
        logger.info(String.format("found %d potential solutions", changes.size()));

        TreeChanges bestModel = findOptimal(changes);
        logger.info("OPTIMAL MODEL: " + bestModel.toString());

        TreeChanges baselineModel = findBaseline(changes);
        logger.info("BEST BASELINE MODEL: " + baselineModel.toString());

        //stop measuring time and compare results
        stopwatch.stop();
        compare(changes, bestModel, baselineModel, stopwatch.elapsed(TimeUnit.SECONDS));

        //return discoved process model
        return PetrinetHelper.ConvertToPetrinet(bestModel.getModifiedProcessTree());
    }

    public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
        Set<Set<T>> sets = new HashSet<Set<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<T>());
            return sets;
        }
        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }
}


