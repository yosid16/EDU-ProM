package org.eduprom.miners.adaptiveNoise;

import org.deckfour.xes.model.XLog;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.exceptions.ProcessTreeConversionException;
import org.eduprom.miners.AbstractPetrinetMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.partitioning.ILogSplitter;
import org.eduprom.partitioning.InductiveLogSplitting;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PetrinetHelper;
import org.eduprom.utils.PocessTreeHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.logging.Level;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

public class TestTreeChangesMiner extends AbstractPetrinetMiner {

    public TestTreeChangesMiner(String filename) throws LogFileNotFoundException {
		super(filename);
	}

    private void modifyPsi(ConformanceInfo info, ProcessTree tree, XLog log) throws MiningException {
        ProcessTree2Petrinet.PetrinetWithMarkings res = null;
        try {
            res = PetrinetHelper.ConvertToPetrinet(tree);
        } catch (ProcessTreeConversionException e) {
            throw new MiningException(e);
        }

        //String path = String.format("./Output/%s_%s_%s" , getName(),
        //        FilenameUtils.removeExtension(Paths.get(filename).getFileName().toString()), changes.id.toString());
        //petrinetHelper.export(res.petrinet, path);

        PNRepResult alignment = petrinetHelper.getAlignment(log, res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        //this.petrinetHelper.printResults(alignment);
        info.setFitness(fitness);

        double precision = petrinetHelper.getPrecision(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
        info.setPrecision(precision);
    }

    @Override
    protected PetrinetWithMarkings minePetrinet() throws MiningException {
        this.logHelper.printLogGrouped(Level.INFO, log);
        return null;
        /*
        float threshold = 1.0f;
        while(threshold <= 100){
            ConformanceInfo info = new ConformanceInfo(0.5, 0.5);
            NoiseInductiveMiner miner = NoiseInductiveMiner.WithNoiseThreshold(filename, threshold);
            modifyPsi(info, miner.mineProcessTree(log), this.log);
            threshold += 0.1f;
            logger.info(String.format("threshold:%f ,fitness: %f, precision: %f, psi: %s",
                    threshold, info.getFitness(), info.getPrecision(), info.getPsi()));
        }

        return  null; //PetrinetHelper.ConvertToPetrinet(target.getProcessTree().toTree());
        */

    }
}
