package org.eduprom.miners.adaptiveNoise;

import com.google.common.collect.Sets;
import org.deckfour.xes.model.XLog;
import org.eduprom.exceptions.MiningException;
import org.eduprom.miners.AbstractMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PocessTreeHelper;
import org.processmining.plugins.NoiseGenerator.Noise;
import org.processmining.plugins.fuzzymodel.miner.FuzzyMinerPlugin;
import org.processmining.processtree.*;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.EdgeImpl;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;

public class TreeChanges {
    protected static final Logger logger = Logger.getLogger(TreeChanges.class.getName());
    protected static final PocessTreeHelper helper = new PocessTreeHelper();

    private final Lock lock = new ReentrantLock();
    private ProcessTree modifiedProcessTree;
    private Partitioning pratitioning;
    private Set<Change> changes;
    private HashMap<UUID, UUID> newIds;
    private Set<UUID> explored;

    private double fitness;
    private double precision;
    private double psi;
    private UUID id;

    public TreeChanges(Partitioning pratitioning){
        this.changes = new HashSet<>();
        this.newIds = new HashMap<>();
        this.pratitioning = pratitioning;
        this.explored = new HashSet<>();
        id = UUID.randomUUID();
        setModifiedProcessTree(pratitioning.getProcessTree().toTree());

        //Set<UUID> ids = pratitioning.getProcessTree().getNodes().stream().map(ProcessTreeElement::getID).collect(Collectors.toSet());
        //Set<UUID> clonedIds = modifiedProcessTree.getNodes().stream().map(x->x.getID()).collect(Collectors.toSet());
    }


    public boolean Add(Change change) throws MiningException {
        Node localNode = modifiedProcessTree.getNode(change.getId());
        this.explored.add(change.getId());
        if (pratitioning.getLogs().containsKey(change.getId()) && localNode != null){
            changes.add(change);
            ProcessTree pt = change.getMiner().mineProcessTree(pratitioning.getLogs().get(change.getId()));
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
        if (pratitioning.getLogs().containsKey(nodeId) && localNode != null){
            changes.put(nodeId, inductiveMiner);
            ProcessTree pt = inductiveMiner.mineProcessTree(pratitioning.getLogs().get(nodeId));
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
        TreeChanges treeChanges = new TreeChanges(pratitioning);
        treeChanges.modifiedProcessTree = this.modifiedProcessTree.toTree();
        treeChanges.changes.addAll(this.changes);
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
        String s = String.format("#Changes %d, psi %f (fitness %f, precision %f)",
                changes.size(), psi, fitness, precision);

        for(Change change : changes){
                s +=  change.toString();
            }

        return s;
    }

    public boolean isBaseline() throws MiningException {
        Optional<Change> entryOptional = changes.stream().findAny();
        if (!entryOptional.isPresent()){
            return false;
            //throw new MiningException("changes must contain at least one element");
        }

        Change minerEntry = entryOptional.get();
        UUID id = newIds.get(minerEntry.getId());
        return modifiedProcessTree.getNode(id) != null && modifiedProcessTree.getNode(id).isRoot() && changes.size() == 1;
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

    public void setFitness(double fitness){
        this.fitness = fitness;
    }

    public double getFitness(){
        return this.fitness;
    }

    public void setPrecision(double precision){
        this.precision = precision;
    }

    public double getPrecision(){
        return this.precision;
    }

    public void setPsi(double psi){
        this.psi = psi;
    }

    public double getPsi(){
        return this.psi;
    }

    public int getNumberOfChanges(){
        return this.changes.size();
    }

    public Partitioning getPratitioning(){
        return this.pratitioning;
    }


    public Stream<AbstractMap.SimpleEntry<UUID, NoiseInductiveMiner>> getUnexplored(List<NoiseInductiveMiner> miners){
        return pratitioning.getLogs().keySet().stream()
                .filter(x-> !this.explored.contains(x))
                .flatMap(x-> miners.stream().map(miner -> new AbstractMap.SimpleEntry<UUID, NoiseInductiveMiner>(x, miner)));
    }

    public Optional<UUID> getUnexploredSingle(){
        return this.pratitioning.getLogs().keySet().stream().filter(x -> !explored.contains(x)).findAny();
    }

    public Set<Change> getChanges(){
        return this.changes;
    }

    public String getChangesDetailed(){
        StringBuilder sb = new StringBuilder();

        changes.stream().sorted(Comparator.comparing(Change:: getId))
                .map(x-> String.format(" id: %s, noise %f", x.getId().toString(), x.getMiner().getNoiseThreshold()))
                .forEach(x-> sb.append(x));

        return sb.toString();
    }


    @Override
    public boolean equals(Object o) {

        return (o instanceof TreeChanges) && getChangesDetailed().equals(((TreeChanges)o).getChangesDetailed());
    }

    @Override
    public int hashCode() {
        return getChangesDetailed().hashCode();
    }
}

