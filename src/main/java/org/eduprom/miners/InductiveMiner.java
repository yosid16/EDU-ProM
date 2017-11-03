package org.eduprom.miners;


import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.exceptions.ParsingException;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.plugins.InductiveMiner.mining.*;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.inductiveVisualMiner.export.ExportModel;
import org.processmining.processtree.ProcessTree;

import java.util.logging.Level;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

public class InductiveMiner extends AbstractPetrinetMiner {

	protected MiningParameters parameters;

	public InductiveMiner(String filename) throws LogFileNotFoundException {
		super(filename);
		this.parameters = new MiningParametersIM();
	}

	public InductiveMiner(String filename, MiningParameters parameters) throws LogFileNotFoundException {
		super(filename);
		this.parameters = parameters;
	}



	@Override
	protected PetrinetWithMarkings minePetrinet() throws MiningException {
		this.logHelper.printLogGrouped(Level.INFO, this.log);

		logger.info("Started mining a petri nets using inductive miner");
		ProcessTree processTree = IMProcessTree.mineProcessTree(log, parameters, getCanceller());
		PetrinetWithMarkings pn = PetrinetHelper.ConvertToPetrinet(processTree);
		ExportModel.exportProcessTree(this.getPromPluginContext(), processTree, "abc");
		//Object[] res = IMPetriNet.minePetriNet(log, parameters, getCanceller());

		logger.info(String.format("Process tree: %s", processTree.toString()));

		return pn;
	}
	/*
	@Override
	protected void readLog() throws ParsingException {
		super.readLog();
	}*/
}