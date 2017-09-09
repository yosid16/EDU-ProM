package org.eduprom.Miners.AdaptiveNoise;

import org.deckfour.xes.model.XLog;
import org.eduprom.Miners.AbstractPetrinetMiner;
import org.eduprom.Miners.AdaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.Miners.InductiveMiner;
import org.eduprom.Partitioning.ILogSplitter;
import org.eduprom.Partitioning.InductiveLogSplitting;
import org.eduprom.Partitioning.Partitioning;
import org.eduprom.Utils.LogHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by ydahari on 6/24/2017.
 */
public class RecursiveScan extends AbstractPetrinetMiner {

    private final Lock lock = new ReentrantLock();

    public RecursiveScan(String filename) throws Exception {
        super(filename);
    }

    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings TrainPetrinet() throws Exception {
        ILogSplitter logSplitter = new InductiveLogSplitting();
        LogHelper helper = new LogHelper();
        XLog log = helper.Read(_filename);
        org.eduprom.Partitioning.Partitioning pratitioning = logSplitter.split(log);

        List<NoiseInductiveMiner> miners = NoiseInductiveMiner.WithNoiseThresholds(_filename, new float[] { (float)0.0, (float)0.1, (float)0.2 })
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
            }
        }

        ProcessTree2Petrinet.PetrinetWithMarkings res = _petrinetHelper.ConvertToPetrinet(bestModel.modifiedProcessTree);

        logger.info("BEST MODEL: " + bestModel.toString());

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
                ProcessTree pt = inductiveMiner.Mine(pratitioning.getLogs().get(node));
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
            treeChanges.modifiedProcessTree = modifiedProcessTree;

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
    }

    public void ModifyPsi(TreeChanges changes, XLog log, double precisionWeight, double fitnessWeight) throws Exception {
        ProcessTree2Petrinet.PetrinetWithMarkings res = _petrinetHelper.ConvertToPetrinet(changes.modifiedProcessTree);

        PNRepResult alignment = _petrinetHelper.getAlignment(log, res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        changes.fitness = fitness;

        double precision = _petrinetHelper.getPrecision(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
        changes.precision = precision;

        changes.psi = precisionWeight * fitness + fitnessWeight * precision;
    }
    public double Psi(NoiseInductiveMiner miner, XLog log, double precisionWeight, double fitnessWeight) throws Exception {
        return precisionWeight * miner.getFitness() + fitnessWeight * miner.getPrecision();

        /*
        new org.processmining.generalizedconformance.algorithms.alignment.PrecisionAligner().measureConformanceAssumingCorrectAlignment()        org.processmining.
        ProcessTree2Petrinet.PetrinetWithMarkings res = _petrinetHelper.ConvertToPetrinet(pt);
        PNRepResult alignment = _petrinetHelper.getAlignment(log, res.petrinet, res.initialMarking, res.finalMarking);
        //AlignmentPrecGenRes conformance = _petrinetHelper.getConformance(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
        PetriNetMetrics metrics = new PetriNetMetrics(_promPluginContext, res.petrinet, res.initialMarking);

        //double pCount = metrics.getMetricValue(PetriNetNofPlacesMetric.NAME);
        double tCount = metrics.getMetricValue(PetriNetNofTransitionsMetric.NAME);

        double simplicity = Math.sqrt(Math.min(  1 / tCount, 1.0));
        //org.processmining.pnanalysis.plugins.PetriNetMetricsPlugin
        //logger.info(String.format("sim: %s, %s, %s, %s",
        //		metrics.getMetricValue(PetriNetStructurednessMetric.NAME),
        //		metrics.getMetricValue(PetriNetNofPlacesMetric.NAME),
        //		metrics.getMetricValue(PetriNetNofTransitionsMetric.NAME),
        //		metrics.getMetricValue(PetriNetNofArcsMetric.NAME)));
        //logger.info(String.format("Simplicity: %s", simplicity));
        //return 0.4 * conformance.getPrecision() + 0.4 *conformance.getGeneralization() + 0.2 * simplicity;
        return Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        */
    }
}


