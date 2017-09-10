package org.eduprom.miners;

import org.eduprom.utils.PetrinetHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.pnanalysis.metrics.impl.PetriNetStructurednessMetric;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

/**
 * Created by ydahari on 4/15/2017.
 */
public abstract class AbstractPetrinetMiner extends AbstractMiner {

    protected PetrinetWithMarkings _petrinet;
    protected PNRepResult _alignment;
    protected PetrinetHelper _petrinetHelper;

    public AbstractPetrinetMiner(String filename) throws Exception {
        super(filename);
        _petrinetHelper = new PetrinetHelper(getPromPluginContext(), getClassifier());
    }

    @Override
    protected void mineSpecific() throws Exception {
        _petrinet = TrainPetrinet();
    }

    public void export() throws Exception {
        _petrinetHelper.Export(_petrinet.petrinet, getOutputPath());
        _petrinetHelper.ExportPnml(_petrinet.petrinet, getOutputPath());
    }

    protected abstract PetrinetWithMarkings TrainPetrinet() throws Exception;

    @Override
    public void evaluate() throws Exception {
        logger.info("Checking conformance");
        _alignment = _petrinetHelper.getAlignment(log, _petrinet.petrinet, _petrinet.initialMarking, _petrinet.finalMarking);
        _petrinetHelper.PrintResults(_alignment);

        AlignmentPrecGenRes conformance = _petrinetHelper.getConformance(log, _petrinet.petrinet, _alignment, _petrinet.initialMarking, _petrinet.finalMarking);
        _petrinetHelper.PrintResults(conformance);

        double v = new PetriNetStructurednessMetric().compute(getPromPluginContext(), _petrinet.petrinet, _petrinet.finalMarking);
        logger.info(String.format("Structuredness: %s", v));
    }
}
