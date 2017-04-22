package org.eduprom.Models;

import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.flowerMiner.FlowerMiner;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

/**
 * Created by ydahari on 22/10/2016.
 */
public class FlowerModel extends AbstractPetrinetModel {

	public FlowerModel(String filename) throws Exception {
		super(filename);
	}
	
    @Override
    protected PetrinetWithMarkings TrainPetrinet() throws Exception {
        Object[] res = new FlowerMiner().mineDefaultPetrinet(_promPluginContext, _log);
        PetrinetWithMarkings pn = new PetrinetWithMarkings();
        pn.petrinet = (PetrinetImpl)res[0];
        pn.initialMarking = (Marking)res[1];
        pn.finalMarking = (Marking)res[2];

        return pn;
    }
}
