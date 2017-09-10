package org.eduprom.miners;

import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

/**
 * Created by ydahari on 22/10/2016.
 */
public class FlowerMiner extends AbstractPetrinetMiner {

	public FlowerMiner(String filename) throws Exception {
		super(filename);
	}
	
    @Override
    protected PetrinetWithMarkings minePetrinet() throws Exception {
        Object[] res = new org.processmining.plugins.flowerMiner.FlowerMiner().mineDefaultPetrinet(getPromPluginContext(), log);
        PetrinetWithMarkings pn = new PetrinetWithMarkings();
        pn.petrinet = (PetrinetImpl)res[0];
        pn.initialMarking = (Marking)res[1];
        pn.finalMarking = (Marking)res[2];

        return pn;
    }
}
