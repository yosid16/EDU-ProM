package org.eduprom.miners.adaptiveNoise;

import org.eduprom.exceptions.MiningException;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PocessTreeHelper;
import org.processmining.processtree.*;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
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

    private ConformanceInfo conformanceInfo;
    private UUID id;

    public TreeChanges(Partitioning pratitioning, double fitnessWeight, double precisionWeight){
        this.changes = new TreeChangesSet();
        this.newIds = new HashMap<>();
        this.pratitioning = pratitioning;
        this.explored = new HashSet<>();
        conformanceInfo = new ConformanceInfo(fitnessWeight, precisionWeight);
        id = UUID.randomUUID();
        setModifiedProcessTree(pratitioning.getProcessTree().toTree());
    }


    public boolean Add(Change change) throws MiningException {
        Node localNode = modifiedProcessTree.getNode(change.getId());
        this.explored.add(change.getId());
        if (pratitioning.getPartitions().containsKey(change.getId()) && localNode != null){
            changes.getChanges().add(change);
            UUID id = change.getId();
            ProcessTree pt = change.getProcessTree();
            //logger.info(String.format("replaced process tree: %s  with: %s", localNode.toString(), pt.toString()));
            helper.merge(pt.getRoot(), localNode);
            newIds.put(change.getId(), pt.getRoot().getID());
            return true;
        }

        return false;
    }
    /*
    public boolean AddWithoutApplying(UUID nodeId, NoiseInductiveMiner inductiveMiner) throws MiningException {
        Node localNode = modifiedProcessTree.getNode(nodeId);
        this.explored.add(nodeId);
        if (pratitioning.getPartitions().containsKey(nodeId) && localNode != null){
            changes.put(nodeId, inductiveMiner);
            ProcessTree pt = inductiveMiner.mineProcessTree(pratitioning.getPartitions().get(nodeId));
            //logger.info(String.format("replaced process tree: %s  with: %s", localNode.toString(), pt.toString()));
            helper.merge(pt.getRoot(), localNode);
            newIds.put(nodeId, pt.getRoot().getID());
            return true;
        }

        return false;
    }

    public void apply() {
        Node localNode = modifiedProcessTree.getNode(nodeId);

    }*/


    public TreeChanges ToTreeChanges() throws MiningException {
        TreeChanges treeChanges = new TreeChanges(pratitioning,
                this.conformanceInfo.getFitnessWeight(), this.conformanceInfo.getPrecisionWeight());
        treeChanges.modifiedProcessTree = this.modifiedProcessTree.toTree();
        treeChanges.changes.getChanges().addAll(this.changes.getChanges());
        treeChanges.newIds.putAll(this.newIds);
        treeChanges.explored.addAll(this.explored);

        /*
        for(Map.Entry<UUID, NoiseInductiveMiner> entry : this.changes.entrySet()){
            if (!treeChanges.Add(entry.getKey(), entry.getValue())){
                throw new MiningException("Failed to clone the tree changes.");
            }
        }

        if (!this.equals(treeChanges)){
            throw new MiningException("clone failed, a non-valid tree changes was produced.");
        }*/

        return treeChanges;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("#Changes %d, psi %f (fitness %f, precision %f); Bits removed: %d",
                changes.getChanges().size(),
                conformanceInfo.getPsi(), conformanceInfo.getFitness(), conformanceInfo.getPrecision(),
                getBitsRemoved()));

        for(Change change : changes.getChanges()){
                sb.append(",");
                sb.append(change.toString());
            }

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
                .map(x-> String.format(" id: %s, noise %f", x.getId().toString(), x.getMiner().getNoiseThreshold()))
                .forEach(x-> sb.append(x));

        return sb.toString();
    }

    public ConformanceInfo getConformanceInfo() {
        return conformanceInfo;
    }

    public int getBitsRemoved(){
        return this.getChanges().getChanges().stream().mapToInt(x->x.getBitsChanged()).sum();
    }

    @Override
    public boolean equals(Object o) {

        return (o instanceof TreeChanges) && getChanges().equals(((TreeChanges)o).getChanges());
    }

    @Override
    public int hashCode() {
        return getChangesDetailed().hashCode();
    }
}

