package org.eduprom.miners;

import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.heuristics.HeuristicsNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.heuristicsnet.miner.heuristics.converter.HeuristicsNetToPetriNetConverter;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.FlexibleHeuristicsMiner;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.settings.HeuristicsMinerSettings;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

/**
 * Created by ydahari on 4/12/2017.
 */
public class HeuristicMiner extends AbstractPetrinetMiner {
    public HeuristicMiner(String filename) throws Exception {
        super(filename);
    }

    @Override
    protected PetrinetWithMarkings minePetrinet() throws Exception {
        logger.info("Started mining a petri nets using heuristic miner");
        HeuristicsMinerSettings settings = new HeuristicsMinerSettings();
        settings.setClassifier(getClassifier());
        FlexibleHeuristicsMiner miner = new FlexibleHeuristicsMiner(getPromPluginContext(), log, settings);
        HeuristicsNet net = miner.mine();
        Object[] res = HeuristicsNetToPetriNetConverter.converter(getPromPluginContext(), net);

        PetrinetWithMarkings pn = new PetrinetWithMarkings();
        pn.petrinet = (PetrinetImpl)res[0];
        pn.initialMarking = (Marking)res[1];
        return pn;
    }
}
