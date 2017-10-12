package org.eduprom.partitioning;

import org.deckfour.xes.model.XLog;
import org.eduprom.miners.adaptiveNoise.ConformanceInfo;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.MiningResult;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.miners.adaptiveNoise.conformance.IConformanceContext;
import org.eduprom.miners.adaptiveNoise.conformance.IConformanceObject;
import org.eduprom.utils.LogHelper;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.ProcessTreeImpl;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

public class Partitioning
{
    public class PartitionInfo implements IConformanceObject {
        private XLog log;
        private Node node;
        private MiningResult miningResult;
        private ConformanceInfo conformanceInfo;
        private IConformanceContext conformanceContext;

        private Stream<Node> getAllRecursiveNodes(Node node){
            if (node instanceof AbstractBlock){
                Stream rec = ((AbstractBlock)node).getChildren().stream().flatMap(this::getAllRecursiveNodes);
                return concat(rec, Stream.of(node));
            }

            return Stream.of(node);
        }

        public PartitionInfo(XLog log, Node node, IConformanceContext conformanceContext){
            this.log = log;
            this.node = node;
            this.conformanceContext = conformanceContext;
        }

        public boolean isRoot() {
            return node.isRoot();
        }

        public int getBits(){
            return log.stream().mapToInt(x -> x.size()).sum();
        }

        public XLog getLog() {
            return log;
        }

        @Override
        public NoiseInductiveMiner getMiner() {
            return conformanceContext.getPartitionMiner();
        }

        @Override
        public void setMiningResult(MiningResult result) {
            this.miningResult = result;
        }

        @Override
        public MiningResult getMiningResult() {
            return this.miningResult;
        }

        @Override
        public UUID getId(){
            return node.getID();
        }

        @Override
        public ConformanceInfo getConformanceInfo() {
            return conformanceInfo;
        }

        @Override
        public void setConformanceInfo(ConformanceInfo conformanceInfo){
            this.conformanceInfo = conformanceInfo;
        }

        public boolean isChildOf(PartitionInfo partitionInfo){
            return this.getAllRecursiveNodes(this.node).filter(x->x.getID() == partitionInfo.getId()).findAny().isPresent();
        }

        public boolean isRalated(PartitionInfo partitionInfo){
            return this.isChildOf(partitionInfo) || partitionInfo.isChildOf(this);
        }

        public Set<UUID> getChildren() {
            return this.getAllRecursiveNodes(this.node).map(x->x.getID()).collect(Collectors.toSet());
        }
    }

    private ProcessTree processTree;
    private  LogHelper _helper;
    private HashMap<UUID, PartitionInfo> logs;
    private IConformanceContext conformanceContext;

    public Partitioning(IConformanceContext conformanceContext)
    {
        _helper = new LogHelper();
        this.processTree = new ProcessTreeImpl();
        logs = new HashMap<UUID, PartitionInfo>();
        this.conformanceContext = conformanceContext;
    }

    @Override
    public String toString() {
        return String.format("Partitioning has: %d sublogs", getPartitions().size());
    }

    public HashMap<UUID, PartitionInfo> getPartitions() {
        return logs;
    }

    public void add(Node node, XLog log){
        this.logs.put(node.getID(), new PartitionInfo(log, node, conformanceContext));
    }

    public ProcessTree getProcessTree() {
        return this.processTree;
    }
}