package org.eduprom.Miners;

import jdk.internal.org.objectweb.asm.util.TraceClassVisitor;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.eduprom.Entities.Trace;
import org.processmining.plugins.InductiveMiner.mining.*;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.processtree.ProcessTree;


import java.util.ArrayList;
import java.util.List;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

public class InductiveMiner extends AbstractPetrinetMiner implements IProcessTreeMiner {


	public InductiveMiner(String filename) throws Exception {
		super(filename);
		_parameters = new MiningParametersIM();
	}

	public InductiveMiner(String filename, MiningParametersIM parameters) throws Exception {
		super(filename);
		_parameters = parameters;
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

	@Override
	public ProcessTree Mine(XLog log) throws Exception {
		logger.info("Started mining a petri nets using inductive miner");
		return IMProcessTree.mineProcessTree(log, _parameters, _canceller);
	}

	public static InductiveMiner WithNoiseThreshold(String filename, float noiseThreshold) throws Exception {
		MiningParametersIM parametersIM = new MiningParametersIM();
		parametersIM.setNoiseThreshold(noiseThreshold);
		return new InductiveMiner(filename, parametersIM);
	}

	public static List<InductiveMiner> WithNoiseThresholds(String filename, float... noiseThreshold) throws Exception {
		ArrayList<InductiveMiner> miners = new ArrayList<>();
		for (float threshold: noiseThreshold){
			miners.add(WithNoiseThreshold(filename, threshold));
		}
		return miners;
	}
}