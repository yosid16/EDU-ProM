package org.eduprom.miners.demo;


import org.deckfour.xes.model.XLog;
import org.eduprom.miners.InductiveMiner;
import org.processmining.log.algorithms.LowFrequencyFilterAlgorithm;
import org.processmining.log.parameters.LowFrequencyFilterParameters;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

public class InductiveVariationDemo extends InductiveMiner {

    public InductiveVariationDemo(String filename) throws Exception {
        super(filename);
    }

    @Override
    protected void readLog() throws Exception {
        XLog log = logHelper.Read(filename);
        LowFrequencyFilterParameters params = new LowFrequencyFilterParameters(log);
        params.setThreshold(20);
        this.log = (new LowFrequencyFilterAlgorithm()).apply(getPromPluginContext(), log, params);
    }

    @Override
    public void evaluate() throws Exception {
        super.evaluate();
        ProcessTree2Petrinet.PetrinetWithMarkings res = getDiscoveredPetriNet();

        PNRepResult alignment = petrinetHelper.getAlignment(log, res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        logger.info(String.format("model fitness: %f", fitness));

        //override how precision is measured, to use ETC plugin:
        // "A fresh look at Precision in Process Conformance by Jorge Munoz-Gama and " +
        //        "Josep Carmona, in Proceedings of Business Processes Management 2010 (BPM 2010)"
        double precision = petrinetHelper.getPrecision(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
        logger.info(String.format("model precision: %f", precision));
    }
}
