package org.eduprom.Miners.AdaptiveNoise.IntermediateMiners;

import org.deckfour.xes.model.XLog;
import org.eduprom.Miners.AbstractPetrinetMiner;
import org.eduprom.Miners.IProcessTreeMiner;
import org.eduprom.Miners.InductiveMiner;
import org.eduprom.Utils.PetrinetHelper;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIM;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.processtree.ProcessTree;

import java.util.ArrayList;
import java.util.List;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

public class NoiseInductiveMiner extends InductiveMiner implements IProcessTreeMiner {

	protected double fitness;
	protected double precision;
	protected ProcessTree processTree;


	public NoiseInductiveMiner(String filename) throws Exception {
		super(filename);
		_parameters = new MiningParametersIM();
	}

	public NoiseInductiveMiner(String filename, MiningParametersIM parameters) throws Exception {
		super(filename);
		_parameters = parameters;
	}

	@Override
	public ProcessTree Mine(XLog log) throws Exception {
		_log = log;
		logger.info("Started mining a petri nets using inductive miner");
		processTree = IMProcessTree.mineProcessTree(log, _parameters, _canceller);
		_petrinet = PetrinetHelper.ConvertToPetrinet(processTree);
		return processTree;
	}

	public static NoiseInductiveMiner WithNoiseThreshold(String filename, float noiseThreshold) throws Exception {
		MiningParametersIM parametersIM = new MiningParametersIM();
		parametersIM.setNoiseThreshold(noiseThreshold);
		return new NoiseInductiveMiner(filename, parametersIM);
	}

	public static List<NoiseInductiveMiner> WithNoiseThresholds(String filename, float... noiseThreshold) throws Exception {
		ArrayList<NoiseInductiveMiner> miners = new ArrayList<>();
		for (float threshold: noiseThreshold){
			miners.add(WithNoiseThreshold(filename, threshold));
		}
		return miners;
	}

	@Override
	public void Evaluate() throws Exception {
		logger.info("Checking alignment");
		_alignment = _petrinetHelper.getAlignment(_log, _petrinet.petrinet, _petrinet.initialMarking, _petrinet.finalMarking);
		_petrinetHelper.PrintResults(_alignment);

		this.fitness = Double.parseDouble(_alignment.getInfo().get("Move-Model Fitness").toString());

		logger.info("Checking precision");
		this.precision = _petrinetHelper.getPrecision(_log, _petrinet.petrinet, _alignment, _petrinet.initialMarking, _petrinet.finalMarking);

		logger.info(String.format("Precision: %S", precision));

		//AlignmentPrecGenRes conformance = _petrinetHelper.getConformance(_log, _petrinet.petrinet, _alignment, _petrinet.initialMarking, _petrinet.finalMarking);
		//_petrinetHelper.PrintResults(conformance);

		//logger.info("Checking Structuredness");
		//double v = new PetriNetStructurednessMetric().compute(_promPluginContext, _petrinet.petrinet, _petrinet.finalMarking);
		//logger.info(String.format("Structuredness: %s", v));
	}

	public double getFitness(){
		return fitness;

	}

	public double getPrecision(){
		return precision;
	}

	public ProcessTree getProcessTree() {
		return processTree;
	}

	public float getNoiseThreshold() {
		return _parameters.getNoiseThreshold();
	}
}