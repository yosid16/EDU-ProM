package org.eduprom.miners.adaptiveNoise.IntermediateMiners;

import org.deckfour.xes.model.XLog;
import org.eduprom.exceptions.ConformanceCheckException;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.ParsingException;
import org.eduprom.miners.IProcessTreeMiner;
import org.eduprom.miners.InductiveMiner;
import org.eduprom.miners.adaptiveNoise.FilterAlgorithm;
import org.processmining.log.algorithms.LowFrequencyFilterAlgorithm;
import org.processmining.log.parameters.LowFrequencyFilterParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIM;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.*;
import java.util.logging.Level;

public class NoiseInductiveMiner extends InductiveMiner {

	protected double fitness;
	protected double precision;
	protected ProcessTree processTree;
	protected HashMap<UUID, MiningResult> processTreeCache = new HashMap<>();


	public NoiseInductiveMiner(String filename, MiningParametersIM parameters) throws LogFileNotFoundException {
		super(filename, parameters);
	}

	public NoiseInductiveMiner(String filename, float noiseThreshold) throws LogFileNotFoundException {
		super(filename);
		this.parameters.setNoiseThreshold(noiseThreshold);
	}

	public MiningResult mineProcessTree(XLog rLog, UUID id) {
		if (processTreeCache.containsKey(id)){
			//logger.info("served from cache");
			return processTreeCache.get(id);
		}
		FilterAlgorithm.FilterResult res = filterLog(rLog);
		//int removed = rLog.size() - filteredLog.size();

		//if (removed > 0){
		//logger.info(String.format("log filtered: original log size: %d, new log size: %d, removed %d traces (threshold: %f)",
		//		rLog.size(), filteredLog.size(), removed, getNoiseThreshold()));
		//}


		processTree = IMProcessTree.mineProcessTree(res.getFilteredLog(), parameters, getCanceller());
		MiningResult result = new MiningResult(processTree, res);
		//logger.info("mined process tree");
		processTreeCache.put(id, result);
		return result;
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

	private FilterAlgorithm.FilterResult filterLog(XLog rLog){
		int noiseThreshold = Math.round(this.getNoiseThreshold() * 100);
		LowFrequencyFilterParameters params = new LowFrequencyFilterParameters(rLog);
		params.setThreshold(noiseThreshold);
		//params.setClassifier(getClassifier());
		return (new FilterAlgorithm()).filter(getPromPluginContext(), rLog, params);
	}

	@Override
	protected void readLog() throws ParsingException {
	}
}