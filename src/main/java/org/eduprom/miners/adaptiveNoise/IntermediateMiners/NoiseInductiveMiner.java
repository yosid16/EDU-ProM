package org.eduprom.miners.adaptiveNoise.IntermediateMiners;

import org.deckfour.xes.model.XLog;
import org.eduprom.benchmarks.IBenchmarkableMiner;
import org.eduprom.exceptions.ConformanceCheckException;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.exceptions.ParsingException;
import org.eduprom.miners.InductiveMiner;
import org.eduprom.miners.adaptiveNoise.ConformanceInfo;
import org.eduprom.miners.adaptiveNoise.FilterAlgorithm;
import org.eduprom.miners.adaptiveNoise.FilterResult;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.log.parameters.LowFrequencyFilterParameters;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIM;
import org.processmining.plugins.InductiveMiner.plugins.IMPetriNet;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.pnanalysis.metrics.impl.PetriNetStructurednessMetric;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import javax.resource.NotSupportedException;
import java.util.*;
import java.util.logging.Level;

public class NoiseInductiveMiner extends InductiveMiner implements IBenchmarkableMiner {


	//region private members
	private HashMap<UUID, MiningResult> processTreeCache = new HashMap<>();
	private ConformanceInfo conformanceInfo;
	private MiningResult result;
	//endregion


	//region constructors
	public NoiseInductiveMiner(String filename, MiningParametersIM parameters) throws LogFileNotFoundException {
		super(filename, parameters);
	}

	public NoiseInductiveMiner(String filename, float noiseThreshold) throws LogFileNotFoundException {
		super(filename);
		this.parameters.setNoiseThreshold(noiseThreshold);
	}
	//endregion

	//region mining for custom log
	public MiningResult mineProcessTree(XLog rLog, UUID id) throws MiningException {
		if (processTreeCache.containsKey(id)){
			return processTreeCache.get(id);
		}

		MiningResult result = mineProcessTree(rLog);
		processTreeCache.put(id, result);
		return result;
	}

	public MiningResult mineProcessTree(XLog rLog) throws MiningException {
		FilterResult res;
		if (rLog.isEmpty()){
			res = new FilterResult((XLog)rLog.clone(), 0, rLog.stream().mapToInt(x->x.size()).sum());
		}
		else{
			res = filterLog(rLog);
		}

		ProcessTree processTree = IMProcessTree.mineProcessTree(res.getFilteredLog(), parameters, getCanceller());
		MiningResult result = new MiningResult(processTree, res);
		return result;
	}
	//endregion

	public static NoiseInductiveMiner withNoiseThreshold(String filename, float noiseThreshold) throws LogFileNotFoundException {
		MiningParametersIM parametersIM = new MiningParametersIM();
		parametersIM.setNoiseThreshold(noiseThreshold);
		return new NoiseInductiveMiner(filename, parametersIM);
	}

	public static List<NoiseInductiveMiner> withNoiseThresholds(String filename, Float... noiseThreshold) throws LogFileNotFoundException {
		ArrayList<NoiseInductiveMiner> miners = new ArrayList<>();
		for (Float threshold: noiseThreshold){
			miners.add(withNoiseThreshold(filename, threshold));
		}
		return miners;
	}

	@Override
	public void evaluate() throws ConformanceCheckException {
	}

	public float getNoiseThreshold() {
		return parameters.getNoiseThreshold();
	}

	private FilterResult filterLog(XLog rLog){
		int noiseThreshold = Math.round(this.getNoiseThreshold() * 100);
		LowFrequencyFilterParameters params = new LowFrequencyFilterParameters(rLog);
		params.setThreshold(noiseThreshold);
		//params.setClassifier(getClassifier());
		return (new FilterAlgorithm()).filter(getPromPluginContext(), rLog, params);
	}

	@Override
	protected ProcessTree2Petrinet.PetrinetWithMarkings minePetrinet() throws MiningException {
		this.result = mineProcessTree(this.log);
		return PetrinetHelper.ConvertToPetrinet(result.getProcessTree());
	}

	@Override
	public ProcessTree2Petrinet.PetrinetWithMarkings getModel() {
		return getDiscoveredPetriNet();
	}

	@Override
	public PetrinetHelper getHelper() {
		return this.petrinetHelper;
	}

	@Override
	public ConformanceInfo getConformanceInfo() {
		return conformanceInfo;
	}

	@Override
	public void setConformanceInfo(ConformanceInfo conformanceInfo) {
		this.conformanceInfo = conformanceInfo;
	}

	public MiningResult getResult() {
		return result;
	}
}