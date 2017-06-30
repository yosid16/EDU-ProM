package org.eduprom.Miners.Matching;

import edu.uci.ics.jung.algorithms.shortestpath.Distance;
import org.deckfour.xes.model.XLog;
import org.eduprom.Miners.IProcessTreeMiner;
import org.eduprom.Partitioning.Partitioning;
import org.processmining.plugins.etm.fitness.FitnessRegistry;
import org.processmining.plugins.etm.model.narytree.NAryTree;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.pnanalysis.metrics.impl.PetriNetNofTransitionsMetric;
import org.processmining.pnanalysis.models.PetriNetMetrics;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ydahari on 6/24/2017.
 */
public class NaiveMatching {

    public class MatchingResult{
        private IProcessTreeMiner matchingProcessTreeMiner;

        private ProcessTree matchingProcessTree;

        public MatchingResult(IProcessTreeMiner matchingProcessTreeMiner, ProcessTree matchingProcessTree){
            this.matchingProcessTreeMiner = matchingProcessTreeMiner;
            this.matchingProcessTree = matchingProcessTree;
        }

        public IProcessTreeMiner getMatchingProcessTreeMiner() {
            return matchingProcessTreeMiner;
        }

        public ProcessTree getMatchingProcessTree() {
            return matchingProcessTree;
        }
    }


    public Map<XLog, MatchingResult> match(Partitioning partitioning, List<IProcessTreeMiner> candidates) throws Exception {
        Map<Node, XLog> partitions = partitioning.getExclusiveLogs();
        Map<XLog, MatchingResult> matching = new HashMap<XLog, MatchingResult>();
        for(Map.Entry<Node, XLog> entry : partitions.entrySet()){
            double bestPsi = 0;
            double currPsi = -1;
            ProcessTree bestModel = null;
            IProcessTreeMiner bestMiner = null;

            for(IProcessTreeMiner miner : candidates){
                ProcessTree processTree = miner.Mine(entry.getValue());
                currPsi = psi(processTree, entry.getValue());
                if (currPsi > bestPsi){
                    bestModel = processTree;
                    bestPsi = currPsi;
                    bestMiner = miner;
                }
            }

            matching.put(entry.getValue(), new MatchingResult(bestMiner, bestModel));
        }

        return matching;
    }

    public double Psi(XLog log, ProcessTree pt) throws Exception {
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
        return 0;
    }


    public double psi(ProcessTree tree, XLog log){
        //org.processmining.generalizedconformance
        //new FitnessRegistry(null).
        //org.processmining.
        return 1;
    }
}


