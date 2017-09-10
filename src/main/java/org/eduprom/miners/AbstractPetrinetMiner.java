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

    //region protected members
    protected PetrinetWithMarkings petrinetWithMarkings;
    protected PNRepResult alignment;
    protected PetrinetHelper petrinetHelper;
    //endregion

    //region constructors

    public AbstractPetrinetMiner(String filename) throws Exception {
        super(filename);
        petrinetHelper = new PetrinetHelper(getPromPluginContext(), getClassifier());
    }

    //endregion

    //region protected methods

    @Override
    protected void mineSpecific() throws Exception {
        petrinetWithMarkings = minePetrinet();
    }

    protected abstract PetrinetWithMarkings minePetrinet() throws Exception;

    //endregion

    //region public methods

    public void export() throws Exception {
        petrinetHelper.Export(petrinetWithMarkings.petrinet, getOutputPath());
        petrinetHelper.ExportPnml(petrinetWithMarkings.petrinet, getOutputPath());
    }

    @Override
    public void evaluate() throws Exception {
        logger.info("Checking conformance");
        alignment = petrinetHelper.getAlignment(log, petrinetWithMarkings.petrinet, petrinetWithMarkings.initialMarking, petrinetWithMarkings.finalMarking);
        petrinetHelper.PrintResults(alignment);

        AlignmentPrecGenRes conformance = petrinetHelper.getConformance(log, petrinetWithMarkings.petrinet, alignment, petrinetWithMarkings.initialMarking, petrinetWithMarkings.finalMarking);
        petrinetHelper.PrintResults(conformance);

        double v = new PetriNetStructurednessMetric().compute(getPromPluginContext(), petrinetWithMarkings.petrinet, petrinetWithMarkings.finalMarking);
        logger.info(String.format("Structuredness: %s", v));
    }

    //endregion
}
