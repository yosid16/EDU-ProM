package org.eduprom.miners;


import org.deckfour.xes.model.XLog;
import org.processmining.log.algorithms.LowFrequencyFilterAlgorithm;
import org.processmining.log.parameters.LowFrequencyFilterParameters;
import org.processmining.plugins.InductiveMiner.mining.*;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

public class InductiveMiner extends AbstractPetrinetMiner {

	protected MiningParametersIM _parameters;

	public InductiveMiner(String filename) throws Exception {
		super(filename);
		_parameters = new MiningParametersIM();
	}

	public InductiveMiner(String filename, MiningParametersIM parameters) throws Exception {
		super(filename);
		_parameters = parameters;
	}



	@Override
	protected PetrinetWithMarkings minePetrinet() throws Exception {
		logger.info("Started mining a petri nets using inductive miner");
		Object[] res = IMPetriNet.minePetriNet(log, _parameters, getCanceller());
		PetrinetWithMarkings pn = new PetrinetWithMarkings();
		pn.petrinet = (PetrinetImpl)res[0];
		pn.initialMarking = (Marking)res[1];
		pn.finalMarking = (Marking)res[2];

		return pn;
	}

	@Override
	protected void readLog() throws Exception {
		XLog log = logHelper.Read(filename);
		LowFrequencyFilterParameters params = new LowFrequencyFilterParameters(log);
		params.setThreshold(20);
		this.log = (new LowFrequencyFilterAlgorithm()).apply(getPromPluginContext(), log, params);
	}
}