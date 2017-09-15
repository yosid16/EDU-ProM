package org.eduprom.miners;

import org.eduprom.exceptions.ConformanceCheckException;
import org.eduprom.exceptions.ExportFailedException;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
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

    public AbstractPetrinetMiner(String filename) throws LogFileNotFoundException {
        super(filename);
        petrinetHelper = new PetrinetHelper(getPromPluginContext(), getClassifier());
    }

    //endregion

    //region protected methods

    @Override
    protected void mineSpecific() throws MiningException {
        petrinetWithMarkings = minePetrinet();
    }

    protected abstract PetrinetWithMarkings minePetrinet() throws MiningException;

    //endregion

    //region public methods

    public void export() throws ExportFailedException {
        petrinetHelper.export(petrinetWithMarkings.petrinet, getOutputPath());
        petrinetHelper.exportPnml(petrinetWithMarkings.petrinet, getOutputPath());
    }

    @Override
    public void evaluate() throws ConformanceCheckException {
        logger.info("Checking conformance");
        alignment = petrinetHelper.getAlignment(log, petrinetWithMarkings.petrinet, petrinetWithMarkings.initialMarking, petrinetWithMarkings.finalMarking);
        petrinetHelper.printResults(alignment);

        AlignmentPrecGenRes conformance = petrinetHelper.getConformance(log, petrinetWithMarkings.petrinet, alignment, petrinetWithMarkings.initialMarking, petrinetWithMarkings.finalMarking);
        petrinetHelper.printResults(conformance);

        double v = new PetriNetStructurednessMetric().compute(getPromPluginContext(), petrinetWithMarkings.petrinet, petrinetWithMarkings.finalMarking);
        logger.info(String.format("Structuredness: %s", v));
    }

    public PetrinetWithMarkings getDiscoveredPetriNet(){
        return this.petrinetWithMarkings;
    }

    //endregion
}
