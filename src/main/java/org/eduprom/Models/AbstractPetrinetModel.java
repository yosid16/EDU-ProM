package org.eduprom.Models;

import org.eduprom.Utils.PetrinetHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.pnanalysis.metrics.impl.PetriNetStructurednessMetric;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;
import java.io.IOException;

/**
 * Created by ydahari on 4/15/2017.
 */
public abstract class AbstractPetrinetModel extends AbstractModel {

    private PetrinetWithMarkings _petrinet;
    private PNRepResult _alignment;
    protected PetrinetHelper _petrinetHelper;

    public AbstractPetrinetModel(String filename) throws Exception {
        super(filename);
        _petrinetHelper = new PetrinetHelper(_promPluginContext, GetClassifier());
    }

    @Override
    protected void TrainSpecific() throws Exception {
        _petrinet = TrainPetrinet();
    }

    public void Export() throws IOException {
        _petrinetHelper.Export(_petrinet.petrinet, GetOutputPath());
    }

    protected abstract PetrinetWithMarkings TrainPetrinet() throws Exception;

    @Override
    public void Evaluate() throws Exception {
        logger.info("Checking conformance");
        _alignment = _petrinetHelper.getAlignment(_log, _petrinet.petrinet, _petrinet.initialMarking, _petrinet.finalMarking);
        _petrinetHelper.PrintResults(_alignment);

        AlignmentPrecGenRes conformance = _petrinetHelper.getConformance(_log, _petrinet.petrinet, _alignment, _petrinet.initialMarking, _petrinet.finalMarking);
        _petrinetHelper.PrintResults(conformance);

        double v = new PetriNetStructurednessMetric().compute(_promPluginContext, _petrinet.petrinet, _petrinet.finalMarking);
        logger.info(String.format("Structuredness: %s", v));
    }
}
