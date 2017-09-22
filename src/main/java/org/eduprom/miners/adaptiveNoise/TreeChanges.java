package org.eduprom.miners.adaptiveNoise;

import com.google.common.collect.Sets;
import org.deckfour.xes.model.XLog;
import org.eduprom.exceptions.MiningException;
import org.eduprom.miners.AbstractMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PocessTreeHelper;
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
    private HashMap<UUID, NoiseInductiveMiner> changes;
    private HashMap<UUID, UUID> newIds;
    private Set<UUID> explored;

    private double fitness;
    private double precision;
    private double psi;
    private UUID id;

    public TreeChanges(Partitioning pratitioning){
        this.changes = new HashMap<>();
        this.newIds = new HashMap<>();
        this.pratitioning = pratitioning;
        this.explored = new HashSet<>();
        id = UUID.randomUUID();
        setModifiedProcessTree(pratitioning.getProcessTree().toTree());

        //Set<UUID> ids = pratitioning.getProcessTree().getNodes().stream().map(ProcessTreeElement::getID).collect(Collectors.toSet());
        //Set<UUID> clonedIds = modifiedProcessTree.getNodes().stream().map(x->x.getID()).collect(Collectors.toSet());
    }

    /***
     *
     * @param nodeId
     * @param inductiveMiner
     * @return true if the replacement of the given node is feasible
     * @throws MiningException
     */
    public boolean Add(UUID nodeId, NoiseInductiveMiner inductiveMiner) throws MiningException {
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


    public TreeChanges ToTreeChanges() throws MiningException {
        TreeChanges treeChanges = new TreeChanges(pratitioning);
        treeChanges.modifiedProcessTree = this.modifiedProcessTree.toTree();
        treeChanges.changes.putAll(this.changes);
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

        for(Map.Entry<UUID, XLog> entry : this.pratitioning.getLogs().entrySet()){
            if (changes.containsKey(entry.getKey())){
                /*
                boolean isRoot = false;
                try{
                    isRoot = this.modifiedProcessTree.getNode(newIds.get(entry.getKey())).isRoot();
                }
                catch (Exception ex){
                    int a = 1;

                }
                */

                s +=  String.format(", node noise %f", changes.get(entry.getKey()).getNoiseThreshold());
                /*
                if (isRoot){
                    s += "(root)";
                }*/
            }
        }

        return s;
    }

    public boolean isBaseline() throws MiningException {
        Optional<Map.Entry<UUID, NoiseInductiveMiner>> entryOptional = changes.entrySet().stream().findAny();
        if (!entryOptional.isPresent()){
            return false;
            //throw new MiningException("changes must contain at least one element");
        }

        Map.Entry<UUID, NoiseInductiveMiner> minerEntry = entryOptional.get();
        UUID id = newIds.get(minerEntry.getKey());
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

    public String getChanges(){
        StringBuilder sb = new StringBuilder();

        changes.entrySet().stream().sorted(Comparator.comparing(Map.Entry:: getKey))
                .map(x-> String.format(" id: %s, noise %f", x.getKey().toString(), x.getValue().getNoiseThreshold()))
                .forEach(x-> sb.append(x));

        return sb.toString();
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

