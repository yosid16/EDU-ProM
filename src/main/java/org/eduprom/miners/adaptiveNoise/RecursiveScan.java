package org.eduprom.miners.adaptiveNoise;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import org.deckfour.xes.model.XLog;
import org.eduprom.exceptions.ExportFailedException;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.exceptions.ProcessTreeConversionException;
import org.eduprom.miners.AbstractPetrinetMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.partitioning.ILogSplitter;
import org.eduprom.partitioning.InductiveLogSplitting;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecursiveScan extends AbstractPetrinetMiner {

    private Float[] noiseThresholds;

    //region private methods

    private void modifyPsi(TreeChanges changes, XLog log, double precisionWeight, double fitnessWeight) throws MiningException {
        ProcessTree2Petrinet.PetrinetWithMarkings res = null;
        try {
            res = PetrinetHelper.ConvertToPetrinet(changes.getModifiedProcessTree());
        } catch (ProcessTreeConversionException e) {
            throw new MiningException(e);
        }

        //String path = String.format("./Output/%s_%s_%s" , getName(),
        //        FilenameUtils.removeExtension(Paths.get(filename).getFileName().toString()), changes.id.toString());
        //petrinetHelper.export(res.petrinet, path);

        PNRepResult alignment = petrinetHelper.getAlignment(log, res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        //this.petrinetHelper.printResults(alignment);
        changes.setFitness(fitness);

        double precision = petrinetHelper.getPrecision(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
        changes.setPrecision(precision);

        changes.setPsi(precisionWeight * fitness + fitnessWeight * precision);
    }

    /*
    private void modifyPsi(ProcessTree processTree, XLog log, double precisionWeight, double fitnessWeight) throws MiningException {
        ProcessTree2Petrinet.PetrinetWithMarkings res = null;
        try {
            res = PetrinetHelper.ConvertToPetrinet(processTree);
        } catch (ProcessTreeConversionException e) {
            throw new MiningException(e);
        }

        PNRepResult alignment = petrinetHelper.getAlignment(log, res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        changes.setFitness(fitness);

        double precision = 0;
        precision = petrinetHelper.getPrecision(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
        changes.setPrecision(precision);

        changes.setPsi(precisionWeight * fitness + fitnessWeight * precision);
    }*/


    private Partitioning splitLog(){

        ILogSplitter logSplitter = new InductiveLogSplitting();
        Partitioning pratitioning = logSplitter.split(this.log);
        return pratitioning;
    }

    private HashSet<TreeChanges> generatePossibleTreeChanges(Partitioning pratitioning) throws MiningException {
        List<NoiseInductiveMiner> miners = NoiseInductiveMiner
                .WithNoiseThresholds(this.filename, this.noiseThresholds)
                .stream().collect(Collectors.toList());
        HashSet<TreeChanges> allChanges = new HashSet<>();
        traverseChildren(allChanges, miners, new TreeChanges(pratitioning), pratitioning.getProcessTree().getRoot());
        return allChanges;
    }

    private HashSet<TreeChanges> generatePossibleTreeChanges2(Partitioning pratitioning) throws MiningException {
        List<NoiseInductiveMiner> miners = NoiseInductiveMiner
                .WithNoiseThresholds(this.filename, this.noiseThresholds)
                .stream().collect(Collectors.toList());
        List<AbstractMap.SimpleEntry<UUID, NoiseInductiveMiner>> options = getOptions(pratitioning, miners).collect(Collectors.toList());

        HashSet<TreeChanges> allChanges = new HashSet<>();
        Stack<TreeChanges> current = new Stack<>();
        current.push(new TreeChanges(pratitioning));
        int scanned = 0;
        while (!current.isEmpty()) {
            TreeChanges currentChange = current.pop();

            //if (currentChange.isBaseline() && currentChange.getNumberOfChanges() > 0){
            //    logger.info(String.format("Scanning a baseline solution"));
            //}

            if (allChanges.add(currentChange)){
                //if (currentChange.isBaseline()){
                //    logger.info(String.format("Found a baseline solution"));
                //}
                options = currentChange.getUnexplored(miners).collect(Collectors.toList());
                for(AbstractMap.SimpleEntry<UUID, NoiseInductiveMiner> entry : options) {
                    UUID id = entry.getKey();
                    NoiseInductiveMiner miner = entry.getValue();

                    //if (pratitioning.getProcessTree().getRoot().getID().equals(id)){
                    //    logger.info(String.format("Checking the root, with noise %f", miner.getNoiseThreshold()));
                    //}

                    TreeChanges newSln = currentChange.ToTreeChanges();
                    if (newSln.Add(id, miner)) { // replacement is feasible
                        if (!allChanges.contains(newSln)){
                            current.push(newSln);
                            scanned++;
                            //logger.info(String.format("scanning: %s", newSln.getChanges().toString()));
                        }
                    }
                }
            }
            else {
                //logger.info(String.format("pruned branch: %s", currentChange.toString()));
                break;
            }

            logger.info(String.format("stack size: %d, scanned: %d", current.size(), scanned));
        }


        allChanges.removeIf(x -> x.getNumberOfChanges() == 0);
        return allChanges;
    }

    public Stream<AbstractMap.SimpleEntry<UUID, NoiseInductiveMiner>> getOptions(Partitioning pratitioning, List<NoiseInductiveMiner> miners){
        return pratitioning.getLogs().keySet().stream()
                .flatMap(x-> miners.stream().map(miner -> new AbstractMap.SimpleEntry<UUID, NoiseInductiveMiner>(x, miner)));
    }

    private TreeChanges findOptimal(HashSet<TreeChanges> treeChanges) throws MiningException {
        double bestPsi = 0;
        TreeChanges bestModel = null;

        for(TreeChanges change : treeChanges) {
            modifyPsi(change, log, 0.5, 0.5);
            logger.info("OPTIONAL MODEL: " + change.toString());

            if (change.getPsi() > bestPsi){
                bestModel = change;
                bestPsi = change.getPsi();
            }
        }

        return bestModel;
    }

    private TreeChanges findBaseline(HashSet<TreeChanges> treeChanges) throws MiningException {
        TreeChanges bestBaseline = null;
        for(TreeChanges change : treeChanges){
            if (!change.isBaseline()){
                continue;
            }

            logger.info("BASELINE MODEL: " + change.toString());
            if (bestBaseline == null || bestBaseline.getPsi() < change.getPsi()){
                bestBaseline = change;
            }
        }
        if(bestBaseline == null){
            throw new MiningException("Could not find a baseline");
        }

        return bestBaseline;
    }

    private void compare(HashSet<TreeChanges> treeChanges, TreeChanges bestModel, TreeChanges baselineModel, long elapsedSeconds) throws ExportFailedException {
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
                    String.valueOf(bestModel.getPsi()), String.valueOf(bestModel.getFitness()), String.valueOf(bestModel.getPrecision()), String.valueOf(bestModel.getNumberOfChanges()),
                    String.valueOf(baselineModel.getPsi()), String.valueOf(baselineModel.getFitness()), String.valueOf(baselineModel.getPrecision()),
                    String.valueOf(bestModel.getPsi() - baselineModel.getPsi())};

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
    }

    //endregion

    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings minePetrinet() throws MiningException {

        //start measuring scan time
        Stopwatch stopwatch = Stopwatch.createStarted();

        //run algorithm
        Partitioning pratitioning = splitLog();
        logger.info(pratitioning.toString());

        HashSet<TreeChanges> changes = generatePossibleTreeChanges2(pratitioning);
        logger.info(String.format("found %d potential solutions", changes.size()));

        TreeChanges baselineModel = findBaseline(changes);
        logger.info("BEST BASELINE MODEL: " + baselineModel.toString());

        TreeChanges bestModel = findOptimal(changes);
        logger.info("OPTIMAL MODEL: " + bestModel.toString());

        //stop measuring time and compare results
        stopwatch.stop();
        compare(changes, bestModel, baselineModel, stopwatch.elapsed(TimeUnit.SECONDS));

        //return discoved process model
        return PetrinetHelper.ConvertToPetrinet(bestModel.getModifiedProcessTree());
    }

    private void traverseChildren(HashSet<TreeChanges> allChanges, List<NoiseInductiveMiner> miners, TreeChanges changes, Node node) throws MiningException {
        for(NoiseInductiveMiner miner : miners){
            TreeChanges newSln = changes.ToTreeChanges();
            if (newSln.Add(node.getID(), miner)){ // replacement is feasible
                if (allChanges.add(newSln)){ //branch that was not scanned
                    logger.info(String.format("found new process tree: %s", newSln.getModifiedProcessTree().toString()));
                }
            }
        }

        if (node instanceof AbstractBlock){
            for(Node childrenNode : ((AbstractBlock) node).getOutgoingEdges()
                    .stream().map(x -> x.getTarget()).collect(Collectors.toList())){
                traverseChildren(allChanges, miners, changes, childrenNode);
            }
        }
    }
}


