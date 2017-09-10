package org.eduprom.miners.adaptiveNoise;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Stopwatch;
import org.deckfour.xes.model.XLog;
import org.eduprom.miners.AbstractPetrinetMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.partitioning.ILogSplitter;
import org.eduprom.partitioning.InductiveLogSplitting;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.LogHelper;
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

/**
 * Created by ydahari on 6/24/2017.
 */
public class RecursiveScan extends AbstractPetrinetMiner {

    private final Lock lock = new ReentrantLock();
    private TreeChanges bestModel;
    private TreeChanges baseline;
    private int optionsScanned;
    private long elapsedSeconds;
    private float[] noiseThresholds;


    public RecursiveScan(String filename, float... noiseThresholds) throws Exception {
        super(filename);
        this.noiseThresholds = noiseThresholds;
    }

    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings minePetrinet() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ILogSplitter logSplitter = new InductiveLogSplitting();
        LogHelper helper = new LogHelper();
        XLog log = helper.Read(filename);
        org.eduprom.partitioning.Partitioning pratitioning = logSplitter.split(log);
        logger.info(pratitioning.toString());

        List<NoiseInductiveMiner> miners = NoiseInductiveMiner.WithNoiseThresholds(filename, this.noiseThresholds)
                .stream().map(x->(NoiseInductiveMiner)x).collect(Collectors.toList());
        List<TreeChanges> changes = TraverseChildren(miners, new TreeChanges(pratitioning), pratitioning.getProcessTree().getRoot());

        double bestPsi = 0;
        double currPsi = -1;
        TreeChanges bestModel = null;

        for(TreeChanges change : changes) {
            ModifyPsi(change, log, 0.5, 0.5);
            logger.info("OPTIONAL MODEL: " + change.toString());

            if (change.psi > bestPsi){
                bestModel = change;
                bestPsi = change.psi;
            }
        }
        TreeChanges baseline = changes.stream().filter(x->x.isBaseline()).findAny().get();
        ProcessTree2Petrinet.PetrinetWithMarkings res = petrinetHelper.ConvertToPetrinet(bestModel.modifiedProcessTree);

        logger.info("BASELINE MODEL: " + baseline.toString());
        logger.info("BEST MODEL: " + bestModel.toString());

        stopwatch.stop();

        this.bestModel = bestModel;
        this.baseline = baseline;
        this.elapsedSeconds = stopwatch.elapsed(TimeUnit.SECONDS);
        this.optionsScanned = changes.size();

        this.petrinetWithMarkings = petrinetHelper.ConvertToPetrinet(bestModel.modifiedProcessTree);

        export();


        return res;
    }

    private List<TreeChanges> TraverseChildren(List<NoiseInductiveMiner> miners, TreeChanges changes, Node node) throws Exception {
        ArrayList<TreeChanges> allChanges = new ArrayList<>();
        for(NoiseInductiveMiner miner : miners){
            TreeChanges newSln = changes.ToTreeChanges();
            if (newSln.Add(node, miner)){
                allChanges.add(newSln);

                if (node instanceof AbstractBlock){
                    for(Node n : ((AbstractBlock) node).getOutgoingEdges().stream().map(x->x.getTarget()).collect(Collectors.toList())){
                        for(TreeChanges res : TraverseChildren(miners, newSln, n)){
                            allChanges.add(res);
                        }
                    }
                }
            }
        }

        return allChanges;
    }

    private class TreeChanges {
        private ProcessTree modifiedProcessTree;
        private Partitioning pratitioning;
        private HashMap<Node, NoiseInductiveMiner> changes;
        private double fitness;
        private double precision;
        private double psi;
        private UUID id;

        public TreeChanges(Partitioning pratitioning){
            this.changes = new HashMap<>();
            this.pratitioning = pratitioning;
            modifiedProcessTree = pratitioning.getProcessTree().toTree();
            id = UUID.randomUUID();
        }

        public boolean Add(Node node, NoiseInductiveMiner inductiveMiner) throws Exception {
            if (pratitioning.getLogs().containsKey(node)){
                changes.put(node, inductiveMiner);
                ProcessTree pt = inductiveMiner.mineProcessTree(pratitioning.getLogs().get(node));
                Merge(modifiedProcessTree.getRoot(), pt.getRoot());
                return true;
            }

            return false;
        }


        public void Merge(Node source, Node target) {
            try{
                lock.lock();
                ProcessTree tree = target.getProcessTree();
                tree.addNode(source);

                if (target.isRoot()){
                    tree.setRoot(source);
                    return;
                }

                for (Block b : target.getParents()){
                    for(Edge e: target.getIncomingEdges()){
                        b.removeOutgoingEdge(e);
                    }
                    b.addChild(source);
                }

                tree.removeNode(source);
            }
            finally {
                lock.unlock();
            }
        }

        public TreeChanges ToTreeChanges() throws Exception {
            TreeChanges treeChanges = new TreeChanges(pratitioning);
            treeChanges.modifiedProcessTree = modifiedProcessTree.toTree();

            for(Map.Entry<Node, NoiseInductiveMiner> entry : this.changes.entrySet()){
                treeChanges.Add(entry.getKey(), entry.getValue());
            }

            return treeChanges;
        }

        @Override
        public String toString() {
            String s = String.format("id: %s, #Changes %d, psi %f (fitness %f, precision %f)",
                    id.toString(), changes.size(), psi, fitness, precision);

            for(Map.Entry<Node, XLog> entry : this.pratitioning.getLogs().entrySet()){
                if (changes.containsKey(entry.getKey())){
                    s +=  String.format(", node noise %f", changes.get(entry.getKey()).getNoiseThreshold());
                }
                else{
                    s +=  String.format(", node noise 0", (float)0.0);
                }

            }

            for(Map.Entry<Node, NoiseInductiveMiner> entry: changes.entrySet()){
                s +=  String.format(", node noise %f", entry.getValue().getNoiseThreshold());
            }
            return s;
        }

        public boolean isBaseline(){
            return changes.values().stream().allMatch(x->x.getNoiseThreshold() == 0);
        }
    }

    @Override
    public void export() throws Exception {
        super.export();
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


        CSVWriter csvWriter = new CSVWriter(new FileWriter(path, true));
        String[] schema = new String[] { "filename", "duration", "options_scanned", "noise_thresholds",
                "best_psi", "best_fitness", "best_precision", "best_sublogs_changed",
                "baseline_psi", "baseline_fitness", "baseline_precision" };

        String[] data = new String[] {filename, String.valueOf(elapsedSeconds), String.valueOf(optionsScanned), StringUtils.join(this.noiseThresholds, ',') ,
                String.valueOf(bestModel.psi), String.valueOf(bestModel.fitness), String.valueOf(bestModel.precision), String.valueOf(bestModel.changes.size()),
                String.valueOf(baseline.psi), String.valueOf(baseline.fitness), String.valueOf(baseline.precision) };

        if (appendSchema){
            csvWriter.writeNext(schema);
        }
        csvWriter.writeNext(data);
        csvWriter.close();
    }

    public void ModifyPsi(TreeChanges changes, XLog log, double precisionWeight, double fitnessWeight) throws Exception {
        ProcessTree2Petrinet.PetrinetWithMarkings res = petrinetHelper.ConvertToPetrinet(changes.modifiedProcessTree);

        //String path = String.format("./Output/%s_%s_%s" , getName(),
        //        FilenameUtils.removeExtension(Paths.get(filename).getFileName().toString()), changes.id.toString());
        //petrinetHelper.export(res.petrinet, path);

        PNRepResult alignment = petrinetHelper.getAlignment(log, res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        changes.fitness = fitness;

        double precision = petrinetHelper.getPrecision(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
        changes.precision = precision;

        changes.psi = precisionWeight * fitness + fitnessWeight * precision;
    }
}


