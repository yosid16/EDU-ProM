package org.eduprom.utils;

import org.eduprom.miners.AbstractMiner;
import nl.tue.astar.AStarException;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.graphvisualizers.plugins.GraphVisualizerPlugin;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.alignetc.AlignETCPlugin;
import org.processmining.plugins.alignetc.result.AlignETCResult;
import org.processmining.plugins.astar.petrinet.AbstractPetrinetReplayer;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithoutILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitness;
import org.processmining.plugins.etm.model.narytree.NAryTree;
import org.processmining.plugins.etm.model.narytree.conversion.ProcessTreeToNAryTree;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.plugins.DotPNGExportPlugin;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.petrinet.replayer.algorithms.IPNReplayParameter;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNMatchInstancesRepResult;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGen;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.plugins.replayer.replayresult.AllSyncReplayResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by ydahari on 4/12/2017.
 */
public class PetrinetHelper {
    protected final static Logger logger = Logger.getLogger(AbstractMiner.class.getName());

    private XEventClassifier _classifier;
    private PluginContext _pluginContext;

    private void validateDirectory(File file){
        File directory = file.getParentFile();
        if (! directory.exists()){
            directory.mkdir();
        }
    }

    public PetrinetHelper(PluginContext pluginContext, XEventClassifier classifier){
        _classifier = classifier;
        _pluginContext = pluginContext;
    }

    public PNRepResult getAlignment(XLog log, PetrinetGraph net, Marking initialMarking, Marking finalMarking) throws Exception {

        Map<Transition, Integer> costMOS = constructMOSCostFunction(net);
        XEventClassifier eventClassifier = _classifier;
        Map<XEventClass, Integer> costMOT = constructMOTCostFunction(net, log, eventClassifier);
        TransEvClassMapping mapping = constructMapping(net, log, eventClassifier);

        AbstractPetrinetReplayer<?, ?> replayEngine = new PetrinetReplayerWithoutILP();

        IPNReplayParameter parameters = new CostBasedCompleteParam(costMOT, costMOS);
        parameters.setInitialMarking(initialMarking);

        if (finalMarking != null){
            parameters.setFinalMarkings(finalMarking);
        }

        parameters.setGUIMode(false);
        parameters.setCreateConn(false);
        parameters.setNumThreads(3);
        ((CostBasedCompleteParam) parameters).setMaxNumOfStates(5000);

        PNRepResult result = null;
        try {
            result = replayEngine.replayLog(_pluginContext, net, log, mapping, parameters);

        } catch (AStarException e) {
            e.printStackTrace();
        }

        return result;
    }

    public PNRepResult getAlignment(XLog log, ProcessTree processTree) throws Exception {
        ProcessTree2Petrinet.PetrinetWithMarkings pt = ConvertToPetrinet(processTree);
        return getAlignment(log, pt.petrinet, pt.initialMarking, pt.finalMarking);

    }
    public AlignmentPrecGenRes getConformance(XLog log, Petrinet net, PNRepResult alignment, Marking initialMarking, Marking finalMarking) throws Exception{
        //Iterator<SyncReplayResult> it = alignment.iterator();
        //while(it.hasNext()){
        //    SyncReplayResult r = it.next();
        //    r.setReliable(true);
        //}
        //org.processmining.pnanalysis.metrics.PetriNetMetricManager.getInstance().getMetrics().get(0).compute()

        AlignmentPrecGen alignmentPrecGen = new AlignmentPrecGen();
        TransEvClassMapping mapping = constructMapping(net, log, _classifier);
        AlignmentPrecGenRes result = alignmentPrecGen.measureConformanceAssumingCorrectAlignment(_pluginContext, mapping, alignment,
                net, initialMarking, false);
        return result;
    }

    public double getPrecision(XLog log, Petrinet net, PNRepResult alignment, Marking initialMarking, Marking finalMarking) throws IllegalTransitionException, ConnectionCannotBeObtained {

        AlignETCPlugin etcPlugin = new AlignETCPlugin();
        TransEvClassMapping mapping = constructMapping(net, log, _classifier);
        EvClassLogPetrinetConnection connection = new EvClassLogPetrinetConnection("", net, log, _classifier, mapping);
        PNMatchInstancesRepResult pNMatchInstancesRepResult = ToPNMatchInstancesRepResult(alignment);
        AlignETCResult res = etcPlugin.checkAlignETCSilent(_pluginContext, log,
                net, initialMarking, finalMarking, connection, pNMatchInstancesRepResult, null, null);
        return res.ap;
    }

    public double getTreeFitness(XLog log, ProcessTree tree){
        CentralRegistry centralRegistry = new CentralRegistry(_pluginContext, log, _classifier,
                new Random());
        NAryTree nAryTree = new ProcessTreeToNAryTree().convert(tree);
        TreeFitness a = centralRegistry.getFitness(nAryTree);
        return 1;


        //FitnessRegistry registry = new FitnessRegistry(_pluginContext);
        //registry.getAllMetricInfos().forEach(x -> x.);

    }

    public PNMatchInstancesRepResult ToPNMatchInstancesRepResult(PNRepResult alignment){
        Collection<AllSyncReplayResult> col = new ArrayList<AllSyncReplayResult>();
        for (SyncReplayResult rep : alignment) {

            //Get all the attributes of the 1-alignment result
            List<List<Object>> nodes = new ArrayList<List<Object>>();
            nodes.add(rep.getNodeInstance());

            List<List<StepTypes>> types = new ArrayList<List<StepTypes>>();
            types.add(rep.getStepTypes());

            SortedSet<Integer> traces = rep.getTraceIndex();
            boolean rel = rep.isReliable();

            //Create a n-alignment result with this attributes
            AllSyncReplayResult allRep = new AllSyncReplayResult(nodes, types, -1, rel);
            allRep.setTraceIndex(traces);//The creator not allow add the set directly
            col.add(allRep);
        }
        return new PNMatchInstancesRepResult(col);
    }

    public void PrintResults(PNRepResult results){
        for(String key : results.getInfo().keySet()) {
            logger.info(String.format("Alignment checking: key= %s, value= %s", key, results.getInfo().get(key)));
        }
    }

    public void PrintResults(AlignmentPrecGenRes conformance){
        logger.info(String.format("Conformance checking, precision %f", conformance.getPrecision()));
        logger.info(String.format("Conformance checking, generalization %f", conformance.getGeneralization()));
    }

    public static ProcessTree2Petrinet.PetrinetWithMarkings ConvertToPetrinet(ProcessTree processTree) throws Exception {
        ProcessTree2Petrinet.PetrinetWithMarkings pn = ProcessTree2Petrinet.convert(processTree);
        return pn;
    }


    private static Map<Transition, Integer> constructMOSCostFunction(PetrinetGraph net) {
        Map<Transition, Integer> costMOS = new HashMap<Transition, Integer>();

        for (Transition t : net.getTransitions())
            if (t.isInvisible())
                costMOS.put(t, 0);
            else
                costMOS.put(t, 1);

        return costMOS;
    }

    private static Map<XEventClass, Integer> constructMOTCostFunction(PetrinetGraph net, XLog log,
                                                                      XEventClassifier eventClassifier) {
        Map<XEventClass, Integer> costMOT = new HashMap<XEventClass, Integer>();
        XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);

        for (XEventClass evClass : summary.getEventClasses().getClasses()) {
            costMOT.put(evClass, 1);
        }

        return costMOT;
    }

    public static TransEvClassMapping constructMapping(PetrinetGraph net, XLog log, XEventClassifier eventClassifier) {
        TransEvClassMapping mapping = new TransEvClassMapping(eventClassifier, new XEventClass("DUMMY", 99999));

        XLogInfo summary = XLogInfoFactory.createLogInfo(log, eventClassifier);

        for (Transition t : net.getTransitions()) {
            for (XEventClass evClass : summary.getEventClasses().getClasses()) {
                String id = evClass.getId();

                if (t.getLabel().equals(id)) {
                    mapping.put(t, evClass);
                    break;
                }
            }

        }

        return mapping;
    }

    public void Export(Petrinet petrinet, String path) throws IOException {
        //fake export from prom plugin :)
        GraphVisualizerPlugin p = new GraphVisualizerPlugin();
        DotPanel panel = (DotPanel)p.apply(_pluginContext, petrinet);
        Dot dot = panel.getDot();
        File file = new File(String.format("%s.png", path));
        validateDirectory(file);

        new DotPNGExportPlugin().exportAsPNG(_pluginContext, dot, file);
    }

    public void ExportPnml(Petrinet petrinet, String path) throws Exception {
        File file = new File(String.format("%s.pnml", path));
        validateDirectory(file);
        new org.processmining.datapetrinets.io.DataPetriNetExporter().exportPetriNetToPNMLFile(_pluginContext,
                DataPetriNet.Factory.fromPetrinet(petrinet), file);
    }
}
