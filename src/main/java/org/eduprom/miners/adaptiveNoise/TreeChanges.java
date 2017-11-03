package org.eduprom.miners.adaptiveNoise;

import org.eduprom.exceptions.MiningException;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PocessTreeHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.processtree.*;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TreeChanges {
    protected static final Logger logger = Logger.getLogger(TreeChanges.class.getName());
    protected static final PocessTreeHelper helper = new PocessTreeHelper();

    //protected static TreeChangesSet treeChangesCache = new TreeChangesSet();

    private final Lock lock = new ReentrantLock();
    private ProcessTree modifiedProcessTree;
    private Partitioning pratitioning;

    private HashMap<UUID, UUID> newIds;
    private Set<UUID> explored;
    private TreeChangesSet changes;

    private ProcessTree2Petrinet.PetrinetWithMarkings petrinetWithMarkings;

    private ConformanceInfo conformanceInfo;
    private PNRepResult alignment;
    private UUID id;

    public TreeChanges(Partitioning pratitioning, ConformanceInfo conformanceInfo){
        this.changes = new TreeChangesSet();
        this.newIds = new HashMap<>();
        this.pratitioning = pratitioning;
        this.explored = new HashSet<>();
        this.conformanceInfo = conformanceInfo;
        this.id = UUID.randomUUID();
        setModifiedProcessTree(pratitioning.getProcessTree().toTree());
    }

    public boolean Add(Change change) throws MiningException {
        Node localNode = modifiedProcessTree.getNode(change.getId());
        this.explored.addAll(change.getReplacedNodes());
        if (pratitioning.getPartitions().containsKey(change.getId()) && localNode != null){

            changes.getChanges().add(change);
            UUID id = change.getId();
            ProcessTree pt = change.getProcessTree();
            //logger.info(String.format("replaced process tree: %s  with: %s", localNode.toString(), pt.toString()));
            //logger.info("new: " + pt.toString());
            //logger.info("old: " + localNode.toString());
            helper.merge(pt.getRoot(), localNode);
            newIds.put(change.getId(), pt.getRoot().getID());
            return true;
        }

        return false;
    }

    public TreeChanges ToTreeChanges() throws MiningException {
        TreeChanges treeChanges = new TreeChanges(pratitioning, this.getConformanceInfo().CloneWeights());
        treeChanges.modifiedProcessTree = this.modifiedProcessTree.toTree();
        treeChanges.changes.getChanges().addAll(this.changes.getChanges());
        treeChanges.newIds.putAll(this.newIds);
        treeChanges.explored.addAll(this.explored);

        return treeChanges;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#Changes %d, psi %f (fitness %f, precision %f, generalization: %f); Bits removed: %d",
                changes.getChanges().size(),
                conformanceInfo.getPsi(),
                conformanceInfo.getFitness(),
                conformanceInfo.getPrecision(),
                conformanceInfo.getGeneralization(),
                this.getBitsRemoved()));

        for(Change change : changes.getChanges()){
                sb.append(",");
                sb.append(change.toString());
            }
        sb.append("MODIFIED TREE: ");
        sb.append(this.getModifiedProcessTree().toString());

        return sb.toString();
    }

    public boolean isBaseline() throws MiningException {
        Optional<Change> entryOptional = changes.getChanges().stream().findAny();
        if (!entryOptional.isPresent()){
            return true;
        }

        Change minerEntry = entryOptional.get();
        UUID id = newIds.get(minerEntry.getId());
        return modifiedProcessTree.getNode(id) != null && modifiedProcessTree.getNode(id).isRoot() && changes.getChanges().size() == 1;
    }

    public void setModifiedProcessTree(ProcessTree pt){
        if (pt == null){
            throw new IllegalArgumentException("process tree cannot be null");
        }
        this.modifiedProcessTree = pt;
    }

    public ProcessTree getModifiedProcessTree(){
        return this.modifiedProcessTree;
    }

    public int getNumberOfChanges(){
        return this.changes.getChanges().size();
    }

    public Partitioning getPratitioning(){
        return this.pratitioning;
    }


    public Stream<Change> getApplicableChanges(Set<Change> baseline){
        return baseline.stream()
                .filter(x -> !this.explored.contains(x.getId()))
                .filter(x -> this.modifiedProcessTree.getNode(x.getId()) != null);
    }

    public TreeChangesSet getChanges(){
        return this.changes;
    }

    public String getChangesDetailed(){
        StringBuilder sb = new StringBuilder();

        changes.getChanges().stream().sorted(Comparator.comparing(Change:: getId))
                .map(x-> String.format(" id: T_%d, noise %f", x.getPartitionInfo().getSequentialId(), x.getMiner().getNoiseThreshold()))
                .forEach(x-> sb.append(x));

        return sb.toString();
    }

    public Map<UUID, Change> getChangesMap(){
        return this.changes.getChanges().stream().collect(Collectors.toMap(y->y.getId(), y->y));
    }

    public Map<UUID, Partitioning.PartitionInfo> getUnchangedPartitions(){
        //Set<UUID> filtered = this.changes.getChanges().stream()
        //        .flatMap(x->x.getPartitionInfo().getChildren().stream()).collect(Collectors.toSet());

        //return this.pratitioning.getPartitions().entrySet().stream()
        //        .filter(x -> !filtered.contains(x.getKey()))
        //        .collect(Collectors.toMap(y -> y.getKey(), y -> y.getValue()));

        Set<UUID> ids = this.getModifiedProcessTree().getNodes().stream().map(y->y.getID()).collect(Collectors.toSet());
        return this.pratitioning.getPartitions().entrySet().stream()
                .filter(x-> ids.contains(x.getKey()))
                .collect(Collectors.toMap(y -> y.getKey(), y -> y.getValue()));
    }

    public ProcessTree2Petrinet.PetrinetWithMarkings getPetrinetWithMarkings() {
        return petrinetWithMarkings;
    }

    public void setPetrinetWithMarkings(ProcessTree2Petrinet.PetrinetWithMarkings petrinetWithMarkings) {
        this.petrinetWithMarkings = petrinetWithMarkings;
    }

    public PNRepResult getAlignment() {
        return alignment;
    }

    public void setAlignment(PNRepResult alignment) {
        this.alignment = alignment;
    }


    /*
    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public XLog getLog() {
        return this.getPratitioning().getOrigianlLog();
    }

    @Override
    public NoiseInductiveMiner getMiner() {
        return null;
    }

    @Override
    public void setMiningResult(MiningResult result) {

    }

    @Override
    public MiningResult getMiningResult() {
        return null;
    }

    @Override
    public void setConformanceInfo(ConformanceInfo conformanceInfo) {

    }
    */
    public ConformanceInfo getConformanceInfo() {
        return conformanceInfo;
    }

    public int getBitsRemoved() {
        int removed = 0;
        for (Change change: getChanges().getChanges()){
            removed += change.getBitsRemoved();
        }
        return removed;
    }

    public int getBits() {
        int removed = 0;
        for (Change change: getChanges().getChanges()){
            removed += change.getBits();
        }
        return removed;
    }

    @Override
    public boolean equals(Object o) {

        return (o instanceof TreeChanges) && getChanges().equals(((TreeChanges)o).getChanges());
    }

    @Override
    public int hashCode() {
        return getChanges().hashCode();
    }
}

