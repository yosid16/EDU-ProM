package org.eduprom.miners.adaptiveNoise.IntermediateMiners;

import org.deckfour.xes.model.XLog;
import org.eduprom.exceptions.ConformanceCheckException;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.ParsingException;
import org.eduprom.miners.IProcessTreeMiner;
import org.eduprom.miners.InductiveMiner;
import org.processmining.log.algorithms.LowFrequencyFilterAlgorithm;
import org.processmining.log.parameters.LowFrequencyFilterParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIM;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class NoiseInductiveMiner extends InductiveMiner implements IProcessTreeMiner {

	protected double fitness;
	protected double precision;
	protected ProcessTree processTree;


	public NoiseInductiveMiner(String filename, MiningParametersIM parameters) throws LogFileNotFoundException {
		super(filename, parameters);
	}

	public NoiseInductiveMiner(String filename, float noiseThreshold) throws LogFileNotFoundException {
		super(filename);
		this.parameters.setNoiseThreshold(noiseThreshold);
	}

	@Override
	public ProcessTree mineProcessTree(XLog rLog) {
		XLog filteredLog = filterLog(rLog);
		int removed = rLog.size() - filteredLog.size();

		//if (removed > 0){
		//logger.info(String.format("log filtered: original log size: %d, new log size: %d, removed %d traces (threshold: %f)",
		//		rLog.size(), filteredLog.size(), removed, getNoiseThreshold()));
		//}


		processTree = IMProcessTree.mineProcessTree(filteredLog, parameters, getCanceller());
		return processTree;
	}

	public static NoiseInductiveMiner WithNoiseThreshold(String filename, float noiseThreshold) throws LogFileNotFoundException {
		MiningParametersIM parametersIM = new MiningParametersIM();
		parametersIM.setNoiseThreshold(noiseThreshold);
		//parametersIM.setRepairLifeCycle(true);
		return new NoiseInductiveMiner(filename, parametersIM);
	}

	public static List<NoiseInductiveMiner> WithNoiseThresholds(String filename, Float... noiseThreshold) throws LogFileNotFoundException {
		ArrayList<NoiseInductiveMiner> miners = new ArrayList<>();
		for (Float threshold: noiseThreshold){
			miners.add(WithNoiseThreshold(filename, threshold));
		}
		return miners;
	}

	@Override
	public void evaluate() throws ConformanceCheckException {
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
		return parameters.getNoiseThreshold();
	}

	private XLog filterLog(XLog rLog){
		int noiseThreshold = Math.round(this.getNoiseThreshold() * 100);
		LowFrequencyFilterParameters params = new LowFrequencyFilterParameters(rLog);
		params.setThreshold(noiseThreshold);
		//params.setClassifier(getClassifier());
		return (new LowFrequencyFilterAlgorithm()).apply(getPromPluginContext(), rLog, params);
	}

	@Override
	protected void readLog() throws ParsingException {
	}
}