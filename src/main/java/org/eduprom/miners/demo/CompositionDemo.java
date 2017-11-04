package org.eduprom.miners.demo;

import org.eduprom.exceptions.ConformanceCheckException;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.miners.AbstractPetrinetMiner;
import org.eduprom.miners.InductiveMiner;
import org.eduprom.miners.alpha.AlphaPlus;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;


public class CompositionDemo extends AbstractPetrinetMiner {
    public CompositionDemo(String filename) throws LogFileNotFoundException {
        super(filename);
    }

    @Override
    protected PetrinetWithMarkings minePetrinet() throws MiningException {
        InductiveMiner inductiveMiner = new InductiveMiner(filename, new MiningParametersIMf());
        AlphaPlus alphaPlusMiner = new AlphaPlus(filename);

        inductiveMiner.mine();
        alphaPlusMiner.mine();

        PetrinetWithMarkings inductiveModel = inductiveMiner.getDiscoveredPetriNet();
        double inductiveScore = myScore(inductiveModel);

        PetrinetWithMarkings alphaPlusModel = alphaPlusMiner.getDiscoveredPetriNet();
        double alphaPlusScore = myScore(alphaPlusModel);

        if (inductiveScore > alphaPlusScore){
            logger.info(String.format("Result: InductiveMiner (score: %f) outperforms Alpha+ (score: %f)", inductiveScore, alphaPlusScore));
            return inductiveModel;
        }
        else {
            logger.info(String.format("Result: Alpha+ (score: %f) outperforms InductiveMiner (score: %f)", alphaPlusScore, inductiveScore));
            return alphaPlusModel;
        }
    }

    public double myScore(PetrinetWithMarkings petrinet) throws ConformanceCheckException {
        PNRepResult alignment = petrinetHelper.getAlignment(log, petrinet.petrinet, petrinet.initialMarking, petrinet.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get("Move-Model Fitness").toString());
        double precision = petrinetHelper.getPrecision(log, petrinet.petrinet, alignment, petrinet.initialMarking, petrinet.finalMarking);

        return 0.5 * fitness + 0.5 * precision;
    }
}
