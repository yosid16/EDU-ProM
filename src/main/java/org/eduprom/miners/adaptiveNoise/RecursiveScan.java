package org.eduprom.miners.adaptiveNoise;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Stopwatch;
import org.apache.commons.math3.stat.descriptive.rank.Median;
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
import org.apache.commons.math3.*;

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

        NoiseInductiveMiner baselineMiner = NoiseInductiveMiner.WithNoiseThreshold(filename, 0f);
        for(Map.Entry<UUID, Partitioning.PartitionInfo> partitionInfo : pratitioning.getPartitions().entrySet()) {
            ConformanceInfo info = new ConformanceInfo(fitnessWeight, precisionWeight);
            ProcessTree pt = baselineMiner.mineProcessTree(partitionInfo.getValue().getLog(), partitionInfo.getKey()).getProcessTree();

            modifyPsi(info, pt, partitionInfo.getValue().getLog());

            partitionInfo.getValue().setProcessTree(pt);
            partitionInfo.getValue().setConformanceInfo(info);

        }
        return pratitioning;
    }

    private Set<TreeChanges> generatePossibleTreeChanges(Partitioning pratitioning, Set<Change> changeOptions) throws MiningException {

        HashMap<TreeChangesSet, TreeChanges> allChanges = new HashMap<>();
        Queue<TreeChanges> current = new LinkedList<>();
        TreeChanges baselineChange = new TreeChanges(pratitioning, fitnessWeight, precisionWeight);
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


    public Set<Change> getOptions(Partitioning pratitioning, List<NoiseInductiveMiner> miners){
        Median median = new Median();
        double[] values = pratitioning.getPartitions().values().stream().mapToDouble(x -> x.getConformanceInfo().getPsi()).toArray();
        double thredhold = median.evaluate(values);

        Set<Change> changes = pratitioning.getPartitions().entrySet().stream()
                .filter(x -> x.getValue().getConformanceInfo().getPsi() < 1.0)
                .flatMap(x-> miners.stream().map(miner -> new Change(x.getKey(), x.getValue().getLog(), miner)))
                .collect(Collectors.toSet());
        changes.stream().forEach(x -> x.discover());

        //changes.removeIf(x -> x.getBitsChanged() == 0);

        return changes;
    }

    private TreeChanges findOptimal(Set<TreeChanges> treeChanges) throws MiningException {
        double bestPsi = 0;
        TreeChanges bestModel = null;
        HashSet<String> scanned = new HashSet<>();


        for(TreeChanges change : treeChanges) {
            String key = change.getModifiedProcessTree().toString();
            if (scanned.contains(key) && !change.isBaseline()){
                continue;
            }
            modifyPsi(change.getConformanceInfo(), change.getModifiedProcessTree(), log);
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

    public RecursiveScan(String filename, double fitnessWeight, double precisionWeight, Float... noiseThresholds) throws Exception {
        super(filename);
        this.noiseThresholds = Stream.concat(Arrays.stream(noiseThresholds)
                ,Stream.of()).distinct().toArray(Float[]::new); //0.0f

        this.miners = NoiseInductiveMiner
                .WithNoiseThresholds(this.filename, this.noiseThresholds)
                .stream().collect(Collectors.toList());

        this.precisionWeight = precisionWeight;
        this.fitnessWeight = fitnessWeight;
    }

    //endregion

    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings minePetrinet() throws MiningException {

        //start measuring scan time
        Stopwatch stopwatch = Stopwatch.createStarted();

        logger.info(String.format("Fitness weight: %f, Precision weight: %f", fitnessWeight, precisionWeight));

        //run algorithm
        Partitioning partitioning = splitLog();
        logger.info(partitioning.toString());

        Set<Change> changeOptions = getOptions(partitioning, miners);
        logger.info(String.format("found %d possible changes to initial partitioning (#miners x #sublogs)", changeOptions.size()));

        if (changeOptions.stream().allMatch(x->x.getBitsChanged() == 0)){
            logger.info(String.format("mined the possible changes, none resulted in filtering the log"));
        }
        logger.info(String.format("mined the possible changes, generating all possible compositions"));

        Set<TreeChanges> changes = generatePossibleTreeChanges(partitioning, changeOptions);
        logger.info(String.format("found %d potential solutions (all subsets of (miners x sublogs) )", changes.size()));

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


