package org.eduprom.miners;

import org.eduprom.utils.PetrinetHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.pnanalysis.metrics.impl.PetriNetStructurednessMetric;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

/***
 * Abstraction layer specifically for petri-nets.
 *
 * Provides exporting capabilities as well as quality metrics.
 *
 * When deriving from this class, the major (if not only)
 * missing piece is to map the log to a petri net.
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

    public PetrinetWithMarkings getDiscoveredPetriNet(){
        return this.petrinetWithMarkings;
    }

    //endregion
}
