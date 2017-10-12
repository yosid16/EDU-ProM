package org.eduprom.utils;

import com.google.common.collect.Lists;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.eduprom.exceptions.ConformanceCheckException;
import org.eduprom.exceptions.ExportFailedException;
import org.eduprom.exceptions.ProcessTreeConversionException;
import org.eduprom.miners.AbstractMiner;
import nl.tue.astar.AStarException;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
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
import java.util.stream.Collectors;

public class PetrinetHelper {

    protected static final Logger logger = Logger.getLogger(AbstractMiner.class.getName());

    //region private members

    private XEventClassifier eventClassifier;
    private PluginContext pluginContext;

    //endregion

    //region private methods

    private void validateDirectory(File file){
        File directory = file.getParentFile();
        if (! directory.exists()){
            directory.mkdir();
        }
    }

    private PNMatchInstancesRepResult toPNMatchInstancesRepResult(PNRepResult alignment){
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

    private static TransEvClassMapping constructMapping(PetrinetGraph net, XLog log, XEventClassifier eventClassifier) {
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


    //endregion

    //region constructors

    public PetrinetHelper(PluginContext pluginContext, XEventClassifier classifier){
        this.eventClassifier = classifier;
        this.pluginContext = pluginContext;
    }

    //endregion

    //region public methods

    public PNRepResult getAlignment(XLog log, PetrinetGraph net, Marking initialMarking, Marking finalMarking) {

        Map<Transition, Integer> costMOS = constructMOSCostFunction(net);
        XEventClassifier eventClassifier = this.eventClassifier;
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
            result = replayEngine.replayLog(pluginContext, net, log, mapping, parameters);

        } catch (AStarException e) {
            e.printStackTrace();
        }

        return result;
    }

    public AlignmentPrecGenRes getConformance(XLog log, Petrinet net, PNRepResult alignment, Marking initialMarking, Marking finalMarking){
        AlignmentPrecGen alignmentPrecGen = new AlignmentPrecGen();
        TransEvClassMapping mapping = constructMapping(net, log, eventClassifier);
        return alignmentPrecGen.measureConformanceAssumingCorrectAlignment(pluginContext, mapping, alignment,
                net, initialMarking, false);
    }

    public double getPrecision(XLog log, Petrinet net, PNRepResult alignment, Marking initialMarking, Marking finalMarking) throws ConformanceCheckException {

        AlignETCPlugin etcPlugin = new AlignETCPlugin();
        TransEvClassMapping mapping = constructMapping(net, log, eventClassifier);
        EvClassLogPetrinetConnection connection = new EvClassLogPetrinetConnection("", net, log, eventClassifier, mapping);
        PNMatchInstancesRepResult pNMatchInstancesRepResult = toPNMatchInstancesRepResult(alignment);
        AlignETCResult res = null;
        try {
            res = etcPlugin.checkAlignETCSilent(pluginContext, log,
                    net, initialMarking, finalMarking, connection, pNMatchInstancesRepResult, null, null);
        } catch (ConnectionCannotBeObtained | IllegalTransitionException e) {
            throw new ConformanceCheckException(e);
        }

        return res.ap;
    }

    public double getGeneralization(XLog log, ProcessTree2Petrinet.PetrinetWithMarkings pt) throws ConformanceCheckException, ProcessTreeConversionException {
        List<List<XTrace>> partitions = Lists.partition((XLogImpl)log, log.size() / 10);

        List<Double> values = new ArrayList<>();
        for(List<XTrace> testTraces: partitions){
            List<XTrace> trainTraces = partitions.stream()
                    .filter(x -> x != testTraces)
                    .flatMap(x->x.stream()).collect(Collectors.toList());
            XLog trainLog = new XLogImpl(log.getAttributes());
            trainLog.addAll(trainTraces);

            XLog testLog = new XLogImpl(log.getAttributes());
            trainLog.addAll(testTraces);

            //ProcessTree2Petrinet.PetrinetWithMarkings pt = ConvertToPetrinet(processTree);

            PNRepResult alignment = getAlignment(testLog, pt.petrinet, pt.initialMarking, pt.finalMarking);
            double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
            values.add(fitness);
        }

        return values.stream().mapToDouble(x->x.floatValue()).sum() / values.size();
    }

    public void printResults(PNRepResult results){
        for(String key : results.getInfo().keySet()) {
            logger.info(String.format("Alignment checking: key= %s, value= %s", key, results.getInfo().get(key)));
        }
    }

    public void printResults(AlignmentPrecGenRes conformance){
        logger.info(String.format("Conformance checking, precision %f", conformance.getPrecision()));
        logger.info(String.format("Conformance checking, generalization %f", conformance.getGeneralization()));
    }

    public static ProcessTree2Petrinet.PetrinetWithMarkings ConvertToPetrinet(ProcessTree processTree) throws ProcessTreeConversionException {
        try {
            return ProcessTree2Petrinet.convert(processTree);
        } catch (Exception e) {
            throw new ProcessTreeConversionException(e);
        }
    }

    public void export(Petrinet petrinet, String path) throws ExportFailedException {
        //fake export from prom plugin :)
        GraphVisualizerPlugin p = new GraphVisualizerPlugin();
        DotPanel panel = (DotPanel)p.apply(pluginContext, petrinet);
        Dot dot = panel.getDot();
        File file = new File(String.format("%s.png", path));
        validateDirectory(file);

        try {
            new DotPNGExportPlugin().exportAsPNG(pluginContext, dot, file);
        } catch (IOException e) {
            throw new ExportFailedException(e);
        }
    }

    public void exportPnml(Petrinet petrinet, String path) throws ExportFailedException {
        File file = new File(String.format("%s.pnml", path));
        validateDirectory(file);
        try {
            new org.processmining.datapetrinets.io.DataPetriNetExporter().exportPetriNetToPNMLFile(pluginContext,
                    DataPetriNet.Factory.fromPetrinet(petrinet), file);
        } catch (Exception e) {
            throw new ExportFailedException(e);
        }
    }

    //endregion
}
