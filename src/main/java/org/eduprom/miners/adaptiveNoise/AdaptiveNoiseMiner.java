package org.eduprom.miners.adaptiveNoise;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.eduprom.benchmarks.IBenchmarkableMiner;
import org.eduprom.entities.CrossValidationPartition;
import org.eduprom.exceptions.MiningException;
import org.eduprom.miners.AbstractPetrinetMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.MiningResult;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.miners.adaptiveNoise.configuration.AdaptiveNoiseConfiguration;
import org.eduprom.miners.adaptiveNoise.conformance.IAdaptiveNoiseConformanceObject;
import org.eduprom.miners.adaptiveNoise.conformance.IConformanceContext;
import org.eduprom.partitioning.ILogSplitter;
import org.eduprom.partitioning.InductiveLogSplitting;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class AdaptiveNoiseMiner extends AbstractPetrinetMiner implements IConformanceContext, IBenchmarkableMiner {

    //region static members

    static final String FITNESS_KEY = "Move-Model Fitness";

    //endregion

    //region private members

    private List<NoiseInductiveMiner> miners;
    private final NoiseInductiveMiner partitionMiner = NoiseInductiveMiner.withNoiseThreshold(filename, 0f);
    private AdaptiveNoiseConfiguration configuration;
    private TreeChanges bestModel;
    private ConformanceInfo conformanceInfo;

    private Set<TreeChanges> changes;

    //endregion

    //region private methods

    private ConformanceInfo getNewConformanceInfo(){
        return new ConformanceInfo(this.configuration.getFitnessWeight(),
                this.configuration.getPrecisionWeight(),
                this.configuration.getGeneralizationWeight());
    }

    private void modifyPsiCrossValidation(IAdaptiveNoiseConformanceObject object) throws MiningException {
        Collections.shuffle(object.getLog());
        int partitionSize = (int)Math.round(object.getLog().size() / 10.0);
        if (partitionSize == 0){
            partitionSize = 1;
        }

        List<CrossValidationPartition> partitions = Lists.partition(object.getLog(), partitionSize)
                .stream().map(x -> new CrossValidationPartition(x, object.getLog().getAttributes())).collect(Collectors.toList());

        List<ConformanceInfo> values = new ArrayList<ConformanceInfo>();
        for(CrossValidationPartition testTraces: partitions){
            List<XTrace> trainTraces = partitions.stream()
                    .filter(x -> x != testTraces)
                    .flatMap(x -> x.getLog().stream()).collect(Collectors.toList());
            XLog trainLog = new XLogImpl(object.getLog().getAttributes());
            trainLog.addAll(trainTraces);

            XLog testLog = new XLogImpl(object.getLog().getAttributes());
            testLog.addAll(testTraces.getLog());

            MiningResult result = object.getMiner().mineProcessTree(trainLog);
            ProcessTree2Petrinet.PetrinetWithMarkings res = PetrinetHelper.ConvertToPetrinet(result.getProcessTree());


            ConformanceInfo candidate = getNewConformanceInfo();

            PNRepResult alignment = petrinetHelper.getAlignment(trainLog, res.petrinet, res.initialMarking, res.finalMarking);
            double fitness = Double.parseDouble(alignment.getInfo().get(FITNESS_KEY).toString());
            candidate.setFitness(fitness);

            double precision = petrinetHelper.getPrecision(trainLog, res.petrinet, alignment, res.initialMarking, res.finalMarking);
            candidate.setPrecision(precision);

            PNRepResult testAlignment = petrinetHelper.getAlignment(testLog, res.petrinet, res.initialMarking, res.finalMarking);
            double generalization = Double.parseDouble(testAlignment.getInfo().get(FITNESS_KEY).toString());
            candidate.setGeneralization(generalization);
            values.add(candidate);
        }

        MiningResult result = object.getMiner().mineProcessTree(object.getLog());
        object.setMiningResult(result);
        ConformanceInfo conformanceInfo = getNewConformanceInfo();
        conformanceInfo.setFitness(values.stream().mapToDouble(x->x.getFitness()).sum() / values.size());
        conformanceInfo.setPrecision(values.stream().mapToDouble(x->x.getPrecision()).sum() / values.size());
        conformanceInfo.setGeneralization(values.stream().mapToDouble(x->x.getGeneralization()).sum() / values.size());
        object.setConformanceInfo(conformanceInfo);
    }

    private void modifyPsiWithGen(IAdaptiveNoiseConformanceObject object) throws MiningException {
        object.setConformanceInfo(getNewConformanceInfo());
        MiningResult result = object.getMiner().mineProcessTree(object.getLog(), object.getId());
        object.setMiningResult(result);
        ProcessTree2Petrinet.PetrinetWithMarkings res = PetrinetHelper.ConvertToPetrinet(result.getProcessTree());

        PNRepResult alignment = petrinetHelper.getAlignment(object.getLog(), res.petrinet, res.initialMarking, res.finalMarking);
        double fitness = Double.parseDouble(alignment.getInfo().get(FITNESS_KEY).toString());
        //this.petrinetHelper.printResults(alignment);
        object.getConformanceInfo().setFitness(fitness);

        AlignmentPrecGenRes alignmentPrecGenRes = petrinetHelper.getConformance(object.getLog(), res.petrinet, alignment, res.initialMarking, res.finalMarking);
        object.getConformanceInfo().setPrecision(alignmentPrecGenRes.getPrecision());
        object.getConformanceInfo().setGeneralization(alignmentPrecGenRes.getGeneralization());
    }

    private Partitioning splitLog(XLog trainLog) throws MiningException {

        ILogSplitter logSplitter = new InductiveLogSplitting(this);
        Partitioning pratitioning = logSplitter.split(trainLog);

        for(Partitioning.PartitionInfo partitionInfo : pratitioning.getPartitions().values()) {
            modifyPsiCrossValidation(partitionInfo);

            logger.info(String.format("Conformance on log split: %s", partitionInfo.getConformanceInfo().toString()));
        }
        return pratitioning;
    }

    private Set<TreeChanges> generatePossibleTreeChanges(Partitioning pratitioning, Set<Change> changeOptions) throws MiningException {

        Set<Set<Change>> changesSet = Sets.newConcurrentHashSet(changeOptions.stream()
                .map(x -> {
                    HashSet<Change> s = new HashSet<>();
                    s.add(x);
                    return s;
                }).collect(Collectors.toList()));

        for(Change change1 : changeOptions) {
            for (Change change2 : changeOptions) {
                if (!change1.getPartitionInfo().isRalated(change2.getPartitionInfo())){
                    changesSet.stream().filter(x->x.contains(change1))
                            .forEach(x->
                            {
                                if (x.stream().allMatch(y -> !y.getPartitionInfo().isRalated(change2.getPartitionInfo()))){
                                    Set<Change> newSln = x.stream().collect(Collectors.toSet());
                                    newSln.add(change2);
                                    changesSet.add(newSln);
                                }
                            });

                    changesSet.stream().filter(x->x.contains(change2))
                            .forEach(x->
                            {
                                if (x.stream().allMatch(y -> !y.getPartitionInfo().isRalated(change1.getPartitionInfo()))){
                                    Set<Change> newSln = x.stream().collect(Collectors.toSet());
                                    newSln.add(change1);
                                    changesSet.add(newSln);
                                }
                            });
                }
            }
        }

        Set<Set<Change>> possibleChanges = new HashSet<>();
        for (Set<Change> changes: changesSet){
            possibleChanges.addAll(Sets.powerSet(changes));
        }

        HashMap<TreeChangesSet, TreeChanges> allChanges = new HashMap<>();
        TreeChanges baselineChange = new TreeChanges(pratitioning, getNewConformanceInfo());
        allChanges.put(baselineChange.getChanges(), baselineChange);

        for (Set<Change> changes: possibleChanges){
            TreeChanges newSln = baselineChange.ToTreeChanges();
            boolean success = true;
            for(Change change : changes) {
                if (!newSln.Add(change)){
                    success = false;
                    break;
                }
            }
            if (success){
                allChanges.put(newSln.getChanges(), newSln);
            }
        }

        Set<String> distinctProcessTrees = new HashSet<>();
        Set<TreeChanges> distinctChanges = new HashSet<>();

        for(TreeChanges change : allChanges.values()){
            String key = change.getModifiedProcessTree().toString();
            if (distinctProcessTrees.add(key) || change.isBaseline()){
                distinctChanges.add(change);
            }
        }

        return distinctChanges;
    }

    private Set<Change> getOptions(Partitioning pratitioning, List<NoiseInductiveMiner> miners) throws MiningException {
        Set<Change> changes = pratitioning.getPartitions().entrySet().stream()
                .filter(x -> !x.getValue().getNode().isLeaf())
                //.filter(x -> x.getValue().getConformanceInfo().getPsi() < 1.0)
                .flatMap(x-> miners.stream()
                        .map(miner -> new Change(x.getValue(), x.getValue().getLog(), miner, this)))
                .collect(Collectors.toSet());
        Map<String, Change> treeToChangeMapping = new HashMap<>();
        for(Change change : changes){
            logger.info(String.format("started change: %s", change.getPartitionInfo().getConformanceInfo().toString()));
            modifyPsiCrossValidation(change);
            String resultingTree = change.getMiningResult().getProcessTree().toString();
            treeToChangeMapping.putIfAbsent(resultingTree, change);

            //logger.info(String.format("Conformance on option: %s", change.getConformanceInfo().toString()));
        }

        return treeToChangeMapping.values().stream().collect(Collectors.toSet());
        //changes.stream().forEach(x -> x.discover());
        //changes.removeIf(x -> x.getBitsChanged() == 0);
    }

    private void calcPsi(Set<TreeChanges> treeChanges, XLog trainLog, XLog testLog) throws MiningException {

        treeChanges.parallelStream().forEach(change -> {
            try {
                ProcessTree2Petrinet.PetrinetWithMarkings res = PetrinetHelper.ConvertToPetrinet(change.getModifiedProcessTree());
                change.setPetrinetWithMarkings(res);
                PNRepResult alignment = petrinetHelper.getAlignment(trainLog, res.petrinet, res.initialMarking, res.finalMarking);
                change.setAlignment(alignment);

                ConformanceInfo info = change.getConformanceInfo();
                double fitness = Double.parseDouble(alignment.getInfo().get(FITNESS_KEY).toString());
                info.setFitness(fitness);

                double precision = petrinetHelper.getPrecision(trainLog, res.petrinet, alignment, res.initialMarking, res.finalMarking);
                info.setPrecision(precision);

                PNRepResult testAlignment = petrinetHelper.getAlignment(testLog, res.petrinet, res.initialMarking, res.finalMarking);
                double generalization = Double.parseDouble(testAlignment.getInfo().get(FITNESS_KEY).toString());
                info.setGeneralization(generalization);

                logger.info(format("OPTIONAL MODEL: %s, tree: %s", change.toString(), change.getModifiedProcessTree().toString()));
            }
            catch (Exception ex){
                throw new RuntimeException(ex);
            }
        });

        TreeChanges bestModel = null;
        for(TreeChanges change : treeChanges) {
            if (bestModel == null || change.getConformanceInfo().getPsi() > bestModel.getConformanceInfo().getPsi()) {
                bestModel = change;
            }
        }
    }

    //endregion

    //region constructors

    public AdaptiveNoiseMiner(String filename, AdaptiveNoiseConfiguration configuration) throws Exception {
        super(filename);
        this.configuration = configuration;

        this.miners = NoiseInductiveMiner
                .withNoiseThresholds(this.filename, configuration.getNoiseThresholds())
                .stream().collect(Collectors.toList());
    }

    //endregion

    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings minePetrinet() throws MiningException {

        List<CrossValidationPartition> crossValidationPartitions =  this.logHelper.crossValidationSplit(this.log, 10);
        CrossValidationPartition testPartition = crossValidationPartitions.stream().findAny().get();
        XLog testLog = testPartition.getLog();
        XLog trainLog = CrossValidationPartition.Bind(crossValidationPartitions.stream().filter(x -> testPartition != x)
                .collect(Collectors.toList())).getLog();

        logger.info(format("Fitness weight: %f, Precision weight: %f, generalization weight: %f",
                configuration.getFitnessWeight(), configuration.getPrecisionWeight(), configuration.getGeneralizationWeight()));

        //run algorithm
        Partitioning partitioning = splitLog(trainLog);//.filter(x -> !x.getNode().isLeaf())
        //partitioning.getPartitions().values().stream().forEach(x-> logger.info(x.toString()));
        logger.info(partitioning.toString());

        Set<Change> changeOptions = getOptions(partitioning, miners);
        logger.info(format("found %d possible changes to initial partitioning (#miners x #sublogs)",
                changeOptions.size()));

        this.changes = generatePossibleTreeChanges(partitioning, changeOptions);

        logger.info(format("found %d distinct trees", changes.size()));

        calcPsi(this.changes, trainLog, testLog);
        logger.info("calculated psi for all trees");

        this.bestModel = changes.stream().filter(x->x.getConformanceInfo().assigned())
                .max(Comparator.comparing(x -> x.getConformanceInfo().getPsi())).get();
        logger.info("OPTIMAL MODEL: " + bestModel.toString());


        //ExportModel.exportProcessTree(getPromPluginContext(), bestModel.getModifiedProcessTree(), "testing");

        //return discoved process model
        return PetrinetHelper.ConvertToPetrinet(bestModel.getModifiedProcessTree());
    }

    @Override
    public NoiseInductiveMiner getPartitionMiner() {
        return this.partitionMiner;
    }

    @Override
    public ProcessTree2Petrinet.PetrinetWithMarkings getModel() {
        return bestModel.getPetrinetWithMarkings();
    }

    @Override
    public PetrinetHelper getHelper() {
        return this.petrinetHelper;
    }

    @Override
    public ConformanceInfo getConformanceInfo() {
        return this.conformanceInfo;
    }

    @Override
    public void setConformanceInfo(ConformanceInfo conformanceInfo) {
        this.conformanceInfo = conformanceInfo;
    }

    public TreeChanges getBestModel() {
        return bestModel;
    }

    public Set<TreeChanges> getChanges() {
        return changes;
    }
}


