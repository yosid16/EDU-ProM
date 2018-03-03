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
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.log.parameters.LowFrequencyFilterParameters;
import org.processmining.plugins.InductiveMiner.dfgOnly.log2logInfo.IMLog2IMLogInfoDefault;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMfa;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinder;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinderIM;
import org.processmining.plugins.InductiveMiner.mining.baseCases.BaseCaseFinderIMi;
import org.processmining.plugins.InductiveMiner.mining.cuts.CutFinder;
import org.processmining.plugins.InductiveMiner.mining.cuts.IMf.CutFinderIMf;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThrough;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThroughIM;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterCombination;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterLoop;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterMaybeInterleaved;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterOr;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterParallel;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterSequenceFiltering;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitterXorFiltering;
import org.processmining.plugins.InductiveMiner.mining.postprocessor.PostProcessor;
import org.processmining.plugins.InductiveMiner.mining.postprocessor.PostProcessorInterleaved;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.*;

import static org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner.FITNESS_KEY;

public class NoiseInductiveMiner extends InductiveMiner implements IBenchmarkableMiner {

	public static class MiningParametersIMipre extends MiningParametersIMf {

		private Partitioning logPartitining;

		public MiningParametersIMipre() {

			setLog2LogInfo(new IMLog2IMLogInfoDefault());

			setBaseCaseFinders(new ArrayList<BaseCaseFinder>(Arrays.asList(
					new BaseCaseFinderIMi(),
					new BaseCaseFinderIM()
			)));

			setCutFinder(new ArrayList<CutFinder>(Arrays.asList(
					//new CutFinderIM(),
					new CutFinderIMf()
			)));

			setLogSplitter(new LogSplitterCombination(
					new LogSplitterXorFiltering(),
					new LogSplitterSequenceFiltering(),
					new LogSplitterParallel(),
					new LogSplitterLoop(),
					new LogSplitterMaybeInterleaved(),
					new LogSplitterParallel(),
					new LogSplitterOr()));

			setFallThroughs(new ArrayList<FallThrough>(Arrays.asList(
					new FallThroughIM()
			)));

			setPostProcessors(new ArrayList<PostProcessor>(Arrays.asList(
					new PostProcessorInterleaved()
			)));

			//set parameters
			setNoiseThreshold((float) 0.0);

			getReduceParameters().setReduceToOr(false);
		}

		public Partitioning getLogPartitining() {
			return logPartitining;
		}

		public void setLogPartitining(Partitioning logPartitining) {
			this.logPartitining = logPartitining;
		}
	}


	//region private members
	private HashMap<UUID, MiningResult> processTreeCache = new HashMap<>();
	private ConformanceInfo conformanceInfo;
	private MiningResult result;

	private boolean filterPreExecution;
	private static MiningParameters getMiningParameters(boolean filterPreExecution, float noiseThreshold){
		if (filterPreExecution){
			return new MiningParametersIMipre() {{ setNoiseThreshold(noiseThreshold); }};
		}
		else{
			return new MiningParametersIMf() {{ setNoiseThreshold(noiseThreshold); }};
		}
	}

	//endregion

	//region constructors

	public NoiseInductiveMiner(String filename, float noiseThreshold, boolean filterPreExecution) throws LogFileNotFoundException {
		super(filename, getMiningParameters(filterPreExecution, noiseThreshold));
		this.filterPreExecution = filterPreExecution;
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
		FilterResult res = new FilterResult(rLog, 0, rLog.stream().mapToInt(x->x.size()).sum());
		ProcessTree processTree = IMProcessTree.mineProcessTree(rLog, this.parameters, getCanceller());
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