package org.eduprom.Miners;

import org.processmining.plugins.InductiveMiner.mining.*;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;

import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;

import org.processmining.models.semantics.petrinet.Marking;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

public class InductiveMiner extends AbstractPetrinetMiner {
	
	public InductiveMiner(String filename) throws Exception {
		super(filename);
		_parameters = new MiningParametersIM();
	}

	private MiningParametersIM _parameters;

	@Override
	protected PetrinetWithMarkings TrainPetrinet() throws Exception {
		logger.info("Started mining a petri nets using inductive miner");
		Object[] res = IMPetriNet.minePetriNet(_log, _parameters, _canceller);
		PetrinetWithMarkings pn = new PetrinetWithMarkings();
		pn.petrinet = (PetrinetImpl)res[0];
		pn.initialMarking = (Marking)res[1];
		pn.finalMarking = (Marking)res[2];

		return pn;
	}
}