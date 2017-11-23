package org.eduprom.miners.adaptiveNoise.IntermediateMiners;

import org.deckfour.xes.model.XLog;
import org.eduprom.benchmarks.IBenchmarkableMiner;
import org.eduprom.benchmarks.configuration.Weights;
import org.eduprom.exceptions.ConformanceCheckException;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.miners.InductiveMiner;
import org.eduprom.miners.adaptiveNoise.AdaMiner;
import org.eduprom.miners.adaptiveNoise.benchmarks.AdaBenchmark;
import org.eduprom.miners.adaptiveNoise.conformance.ConformanceInfo;
import org.eduprom.miners.adaptiveNoise.filters.FilterAlgorithm;
import org.eduprom.miners.adaptiveNoise.filters.FilterResult;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.log.parameters.LowFrequencyFilterParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.*;

import static org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner.FITNESS_KEY;

public class NoiseInductiveMiner extends InductiveMiner implements IBenchmarkableMiner {


	//region private members
	private HashMap<UUID, MiningResult> processTreeCache = new HashMap<>();
	private ConformanceInfo conformanceInfo;
	private MiningResult result;

	private boolean filterPreExecution;
	//endregion

	/*
	private ConformanceInfo getPsi(ProcessTree processTree, XLog log) throws MiningException {
		ConformanceInfo info = new ConformanceInfo(Weights.getUniform());
		ProcessTree2Petrinet.PetrinetWithMarkings res = PetrinetHelper.ConvertToPetrinet(processTree);
		PNRepResult alignment = petrinetHelper.getAlignment(log, res.petrinet, res.initialMarking, res.finalMarking);
		double fitness = Double.parseDouble(alignment.getInfo().get(FITNESS_KEY).toString());
		//this.petrinetHelper.printResults(alignment);
		info.setFitness(fitness);

		AlignmentPrecGenRes alignmentPrecGenRes = petrinetHelper.getConformance(log, res.petrinet, alignment, res.initialMarking, res.finalMarking);
		info.setPrecision(alignmentPrecGenRes.getPrecision());
		info.setGeneralization(alignmentPrecGenRes.getGeneralization());
		return info;
	}*/
	//region constructors

	public NoiseInductiveMiner(String filename, float noiseThreshold, boolean filterPreExecution) throws LogFileNotFoundException {
		super(filename, new MiningParametersIMf() {{ setNoiseThreshold(noiseThreshold); }});
		this.filterPreExecution = filterPreExecution;
		/*
		this.parameters.setCutFinder(new ArrayList<CutFinder>(Arrays.asList(
				//new CutFinderIM(),
				new CutFinderIMf()
		)));
		*/
	}
	//endregion

	//region mining for custom log
	public MiningResult mineProcessTree(XLog rLog, UUID id) throws MiningException {
		if (processTreeCache.containsKey(id)){
			return processTreeCache.get(id);
		}

		MiningResult res = mineProcessTree(rLog);
		processTreeCache.put(id, res);
		return res;
	}

	public MiningResult mineProcessTree(XLog rLog) throws MiningException {
		FilterResult res = null;
		MiningParameters runParams = null;
		if (filterPreExecution){
			res = filterLog(rLog);
			runParams = new MiningParametersIMf() {{ setNoiseThreshold(0); }};
		}
		else if (rLog.isEmpty()){
			res = new FilterResult((XLog)rLog.clone(), 0, rLog.stream().mapToInt(x->x.size()).sum());
		}
		else{
			res = new FilterResult(rLog, 0, rLog.stream().mapToInt(x->x.size()).sum());
			runParams = this.parameters;
		}

		ProcessTree processTree = IMProcessTree.mineProcessTree(res.getFilteredLog(), runParams, getCanceller());
		this.result = new  MiningResult(processTree, res);
		return this.result;
	}
	//endregion


	public static List<NoiseInductiveMiner> withNoiseThresholds(String filename, boolean filterPreExecution, float... noiseThreshold) throws LogFileNotFoundException {
		ArrayList<NoiseInductiveMiner> miners = new ArrayList<>();
		for (Float threshold: noiseThreshold){
			miners.add(new NoiseInductiveMiner(filename, threshold, filterPreExecution));
		}
		return miners;
	}

	@Override
	public void evaluate() throws MiningException {
		setConformanceInfo(AdaBenchmark.getPsi(petrinetHelper, this.result.getProcessTree(), this.log, Weights.getUniform()));
		logger.info(String.format("Inductive Miner Infrequent - conformance: %s, tree: %s", this.getConformanceInfo(), this.result.getProcessTree()));
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

	public boolean isFilterPreExecution() {
		return filterPreExecution;
	}

	@Override
	public ProcessTree getProcessTree() {
		return this.result.getProcessTree();
	}
}