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
import org.eduprom.partitioning.InductiveCutSplitting;
import org.eduprom.partitioning.trunk.InductiveLogSplitting;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.reverseOrder;

public class AdaptiveNoiseMiner extends AbstractPetrinetMiner implements IConformanceContext, IBenchmarkableMiner {

    //region static members

    static final String FITNESS_KEY = PNRepResult.TRACEFITNESS;

    //endregion

    //region private members

    private List<NoiseInductiveMiner> miners;
    private final NoiseInductiveMiner partitionMiner = new NoiseInductiveMiner(filename, 0f, false);
    private AdaptiveNoiseConfiguration configuration;
    private TreeChanges bestModel;
    private ConformanceInfo conformanceInfo;

    private Map<String, TreeChanges> changes;

    //endregion

    //region private methods

    private ConformanceInfo getNewConformanceInfo(){
        return new ConformanceInfo(this.configuration.getWeights().getFitnessWeight(),
                this.configuration.getWeights().getPrecisionWeight(),
                this.configuration.getWeights().getGeneralizationWeight());
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
            if (trainTraces.isEmpty()){
                trainTraces.addAll(testTraces.getLog().stream().collect(Collectors.toList()));
            }

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

    private List<Partitioning> splitLog(XLog trainLog, boolean computeConformance) throws MiningException {
        List<Partitioning> partitionings = new ArrayList<>();
        for(float noiseThreshold: this.configuration.getNoiseThresholds()){
            partitionings.add(splitLog(trainLog, computeConformance, noiseThreshold));
        }
        return partitionings;
    }
    //splitLog(pratitioning.getPartitions().get(UUID.fromString("1d78cd13-f981-48c3-8a56-7b492f689b4f")).getLog(), false, 0.1f).getPartitions()
    private Partitioning splitLog(XLog trainLog, boolean computeConformance, float noiseFiltering) throws MiningException {
        ILogSplitter logSplitter = new InductiveCutSplitting(this, noiseFiltering);
        Partitioning pratitioning = logSplitter.split(trainLog);

        //for(Partitioning.PartitionInfo partitionInfo: pratitioning.getPartitions().values()){
        //    logger.info(partitionInfo.toString());
        //}
        if (computeConformance){
            for(Partitioning.PartitionInfo partitionInfo : pratitioning.getPartitions().values()) {
                MiningResult result = partitionInfo.getMiner().mineProcessTree(partitionInfo.getLog());
                partitionInfo.setMiningResult(result);
                modifyPsiCrossValidation(partitionInfo);

                logger.log(Level.FINE, String.format("Conformance on log split: %s", partitionInfo.getConformanceInfo().toString()));
            }
        }

        return pratitioning;
    }

    private TreeChanges apply(TreeChanges baselineChange, Set<Change> changes) throws MiningException {
        TreeChanges newSln = baselineChange.ToTreeChanges();
        boolean success = true;
        for(Change change : changes) {
            if (!newSln.Add(change)){
                success = false;
                break;
            }
        }
        if (success){
            return newSln;
        }
        else{
            return null;
        }
    }

    private Map<String, TreeChanges> generatePossibleTreeChanges(Partitioning pratitioning, Set<Change> changeOptions) throws MiningException {
        int maxSize = 5;
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
                                    if (newSln.size() <= maxSize){
                                        changesSet.add(newSln);
                                    }
                                }
                            });

                    changesSet.stream().filter(x->x.contains(change2))
                            .forEach(x->
                            {
                                if (x.stream().allMatch(y -> !y.getPartitionInfo().isRalated(change1.getPartitionInfo()))){
                                    Set<Change> newSln = x.stream().collect(Collectors.toSet());
                                    newSln.add(change1);
                                    if (newSln.size() <= maxSize){
                                        changesSet.add(newSln);
                                    }
                                }
                            });
                }
            }
        }

        Set<Set<Change>> possibleChanges = new HashSet<>();
        for (Set<Change> changes: changesSet){
            possibleChanges.addAll(Sets.powerSet(changes));
        }

        Map<TreeChangesSet, TreeChanges> allChanges = new ConcurrentHashMap<>();
        TreeChanges baselineChange = new TreeChanges(pratitioning, getNewConformanceInfo());
        allChanges.put(baselineChange.getChanges(), baselineChange);

        possibleChanges.parallelStream()
                .map(x -> {
                    try {
                        return apply(baselineChange, x);
                    } catch (MiningException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(Objects::nonNull)
                .forEach(x -> allChanges.put(x.getChanges(), x));

        /*
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
        }*/
        Map<String, TreeChanges> treeChangesMap = new HashMap<>();
        allChanges.values().forEach(x-> treeChangesMap.putIfAbsent(x.getModifiedProcessTree().toString(), x));
        return treeChangesMap;
    }

    private Set<Change> getOptions(Partitioning pratitioning, List<NoiseInductiveMiner> miners, boolean computeConformance) throws MiningException {
        Set<Change> changes = pratitioning.getPartitions().entrySet().stream()
                .filter(x -> !x.getValue().getNode().isLeaf())
                //.filter(x -> x.getValue().getConformanceInfo().getPsi() < 1.0)
                .flatMap(x-> miners.stream()
                        .map(miner -> new Change(x.getValue(), x.getValue().getLog(), miner, this)))
                .collect(Collectors.toSet());
        Map<String, Change> treeToChangeMapping = new HashMap<>();
        for(Change change : changes){
            //save the resulting subtree
            MiningResult result = change.getMiner().mineProcessTree(change.getLog());
            change.setMiningResult(result);

            String resultingTree = change.getMiningResult().getProcessTree().toString();
            //logger.log(Level.INFO, String.format("Partition: %d, Tree: %s", change.getPartitionInfo().getSequentialId() ,resultingTree));
            treeToChangeMapping.putIfAbsent(resultingTree, change);

            if (computeConformance){
                modifyPsiCrossValidation(change);
            }
        }

        return treeToChangeMapping.values().stream().collect(Collectors.toSet());
    }

    private double getPruneThreshold(){
        if (bestModel == null){
            return 0;
        }
        return bestModel.getConformanceInfo().getPsi();
    }

    public synchronized void checkBestPsi(TreeChanges change){
        if (bestModel == null || change.getConformanceInfo().getPsi() > bestModel.getConformanceInfo().getPsi()) {
            bestModel = change;
        }
    }
    private void calcPsi(Collection<TreeChanges> treeChanges, XLog trainLog, XLog testLog) throws MiningException {
        AtomicInteger progress = new AtomicInteger();
        treeChanges.parallelStream().forEach(change -> {
            try {
                Stopwatch stopwatch = Stopwatch.createStarted();
                ProcessTree2Petrinet.PetrinetWithMarkings res = PetrinetHelper.ConvertToPetrinet(change.getModifiedProcessTree());
                change.setPetrinetWithMarkings(res);
                PNRepResult alignment = petrinetHelper.getAlignment(trainLog, res.petrinet, res.initialMarking, res.finalMarking);
                change.setAlignment(alignment);
                stopwatch.stop();

                ConformanceInfo info = change.getConformanceInfo();
                double fitness = Double.parseDouble(alignment.getInfo().get(FITNESS_KEY).toString());
                info.setFitness(fitness);
                info.setFitnessDuration(stopwatch.elapsed(TimeUnit.MILLISECONDS));

                int value = progress.incrementAndGet();
                if (value % 100 == 0){
                    logger.info(String.format("calculated fitness for %d", value));
                }
            }
            catch (Exception ex){
                throw new RuntimeException(ex);
            }
        });

        AtomicInteger pruned = new AtomicInteger();
        pruned.set(0);
        progress.set(0);
        treeChanges.stream().sorted(Comparator.comparing(x -> x.getConformanceInfo().minValue(), reverseOrder()))
                .parallel().forEachOrdered(change -> {
            int value = progress.incrementAndGet();
            try {
                ProcessTree2Petrinet.PetrinetWithMarkings res = change.getPetrinetWithMarkings();
                PNRepResult alignment = change.getAlignment();
                ConformanceInfo info = change.getConformanceInfo();


                if (info.maxValue() >=  getPruneThreshold()){
                    Stopwatch stopwatch = Stopwatch.createStarted();
                    PNRepResult testAlignment = petrinetHelper.getAlignment(testLog, res.petrinet, res.initialMarking, res.finalMarking);
                    double generalization = Double.parseDouble(testAlignment.getInfo().get(FITNESS_KEY).toString());
                    info.setGeneralization(generalization);
                    stopwatch.stop();
                    info.setGeneralizationDuration(stopwatch.elapsed(TimeUnit.MILLISECONDS));
                }
                else{
                    info.setGeneralization(0.0);
                    pruned.incrementAndGet();
                    //logger.log(Level.INFO,"pruned");
                    return;
                }

                if (info.maxValue() >=  getPruneThreshold()){
                    Stopwatch stopwatch = Stopwatch.createStarted();
                    double precision = petrinetHelper.getPrecision(trainLog, res.petrinet, alignment, res.initialMarking, res.finalMarking);
                    info.setPrecision(precision);
                    stopwatch.stop();
                    info.setPrecisionDuration(stopwatch.elapsed(TimeUnit.MILLISECONDS));
                }
                else{
                    info.setPrecision(0.0);
                    pruned.incrementAndGet();
                    //logger.log(Level.INFO,"pruned");
                    return;
                }

                checkBestPsi(change);

                //logger.log(Level.INFO, format("OPTIONAL MODEL: %s, tree: %s", change.toString(), change.getModifiedProcessTree().toString()));
            }
            catch (Exception ex){
                throw new RuntimeException(ex);
            }
            finally {
                if (value % 100 == 0){
                    logger.info(String.format("calculated psi for %d trees, pruned %d", value, pruned.intValue()));
                }
            }
        });


        logger.info(String.format("calculation time: fitness %d, precision: %d, generalization %d",
                treeChanges.stream().mapToLong(x->x.getConformanceInfo().getFitnessDuration()).sum(),
                treeChanges.stream().mapToLong(x->x.getConformanceInfo().getPrecisionDuration()).sum(),
                treeChanges.stream().mapToLong(x->x.getConformanceInfo().getGeneralizationDuration()).sum()));

        logger.info(String.format("calculated psi for %d trees, pruned %d", treeChanges.size(), pruned.intValue()));
    }

    //endregion

    //region constructors

    public AdaptiveNoiseMiner(String filename, AdaptiveNoiseConfiguration configuration) throws Exception {
        super(filename);
        this.configuration = configuration;
        this.changes = new HashMap<>();
        this.miners = NoiseInductiveMiner
                .withNoiseThresholds(this.filename, configuration.isPreExecuteFilter(), configuration.getNoiseThresholds())
                .stream().collect(Collectors.toList());
    }

    //endregion

    //region protected methods
    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings minePetrinet() throws MiningException {

        List<CrossValidationPartition> crossValidationPartitions =  this.logHelper.crossValidationSplit(this.log, 10);
        List<TreeChanges> models = new ArrayList<>();
        for(CrossValidationPartition testPartition: crossValidationPartitions){
            XLog testLog = testPartition.getLog();
            XLog trainLog = CrossValidationPartition.Bind(crossValidationPartitions.stream().filter(x -> testPartition != x)
                    .collect(Collectors.toList())).getLog();

            logger.info(String.format("Miners with %d noise thresholds %s are optional",
                    miners.size(), miners.stream().map(x-> String.valueOf(x.getNoiseThreshold())).collect(Collectors.joining (","))));

            logger.info(format("Fitness weight: %f, Precision weight: %f, generalization weight: %f",
                    configuration.getWeights().getFitnessWeight(), configuration.getWeights().getPrecisionWeight(), configuration.getWeights().getGeneralizationWeight()));

            //run algorithm
            Partitioning partitioning = splitLog(trainLog, false, configuration.getPartitionNoiseFilter());
            logger.info(partitioning.toString());
            Set<Change> changeOptions = getOptions(partitioning, miners, false);
            logger.info(format("found %d possible changes to initial partitioning (#miners x #sublogs)",
                    changeOptions.size()));

            Map<String, TreeChanges> treeTochanges = generatePossibleTreeChanges(partitioning, changeOptions);
            treeTochanges.entrySet().forEach(x -> this.changes.putIfAbsent(x.getKey(), x.getValue()));

            /*
            List<Partitioning> allPartitioning = splitLog(trainLog, false);
            //partitioning.getPartitions().values().stream().forEach(x-> logger.info(x.toString()));

            for(Partitioning partitioning : allPartitioning){
                logger.info(partitioning.toString());
                Set<Change> changeOptions = getOptions(partitioning, miners, false);
                logger.info(format("found %d possible changes to initial partitioning (#miners x #sublogs)",
                        changeOptions.size()));

                Map<String, TreeChanges> treeTochanges = generatePossibleTreeChanges(partitioning, changeOptions);
                treeTochanges.entrySet().forEach(x -> this.changes.putIfAbsent(x.getKey(), x.getValue()));
            }*/


            logger.info(format("found %d distinct trees", changes.size()));

            calcPsi(this.changes.values(), trainLog, testLog);
            logger.info("calculated psi for all trees");
            logger.info("OPTIMAL MODEL: " + bestModel.toString());

            if (!configuration.getUseCrossValidation()){
                return PetrinetHelper.ConvertToPetrinet(bestModel.getModifiedProcessTree());
            }
            else{
                models.add(bestModel);
                this.bestModel = null;
            }
            //return discoved process model
        }

        this.bestModel = models.stream().max(Comparator.comparing(x->x.getConformanceInfo().getPsi())).get();
        return PetrinetHelper.ConvertToPetrinet(bestModel.getModifiedProcessTree());
    }

    //endregion

    //region public methods

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

    public Collection<TreeChanges> getChanges() {
        return changes.values();
    }

    public AdaptiveNoiseConfiguration getConfiguration() {
        return configuration;
    }

    //endregion
}


