package org.eduprom.miners;


import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.processmining.plugins.InductiveMiner.mining.*;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.logging.Level;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

public class InductiveMiner extends AbstractPetrinetMiner {

	protected MiningParametersIM parameters;

	public InductiveMiner(String filename) throws LogFileNotFoundException {
		super(filename);
		this.parameters = new MiningParametersIM();
	}

	public InductiveMiner(String filename, MiningParametersIM parameters) throws LogFileNotFoundException {
		super(filename);
		this.parameters = parameters;
	}



	@Override
	protected PetrinetWithMarkings minePetrinet() throws MiningException {
		this.logHelper.printLogGrouped(Level.INFO, this.log);
		logger.info("Started mining a petri nets using inductive miner");
		Object[] res = IMPetriNet.minePetriNet(log, parameters, getCanceller());
		PetrinetWithMarkings pn = new PetrinetWithMarkings();
		pn.petrinet = (PetrinetImpl)res[0];
		pn.initialMarking = (Marking)res[1];
		pn.finalMarking = (Marking)res[2];

		return pn;
	}
}