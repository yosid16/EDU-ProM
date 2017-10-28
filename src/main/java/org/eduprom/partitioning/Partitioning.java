package org.eduprom.partitioning;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.eduprom.miners.adaptiveNoise.ConformanceInfo;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.MiningResult;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.miners.adaptiveNoise.conformance.IAdaptiveNoiseConformanceObject;
import org.eduprom.miners.adaptiveNoise.conformance.IConformanceContext;
import org.eduprom.utils.LogHelper;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.ProcessTreeImpl;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;

public class Partitioning
{
    public static int nextPartitionId = 1;

    public class PartitionInfo implements IAdaptiveNoiseConformanceObject {
        private XLog partitionLog;
        private Node node;
        private MiningResult miningResult;
        private ConformanceInfo conformanceInfo;
        private IConformanceContext conformanceContext;
        private Set<UUID> resursiveChildern;

        private Stream<Node> getAllRecursiveNodes(Node node){
            if (node instanceof AbstractBlock){
                Stream rec = ((AbstractBlock)node).getChildren().stream().flatMap(this::getAllRecursiveNodes);
                return concat(rec, Stream.of(node));
            }

            return Stream.of(node);
        }

        public PartitionInfo(XLog partitionLog, Node node, IConformanceContext conformanceContext){
            this.partitionLog = partitionLog;
            this.node = node;
            this.conformanceContext = conformanceContext;
            this.resursiveChildern = getAllRecursiveNodes(this.node).map(x->x.getID()).collect(Collectors.toSet());
        }

        public boolean isRoot() {
            return node.isRoot();
        }

        public int getBits(){
            return partitionLog.stream().mapToInt(x -> x.size()).sum();
        }

        public XLog getPartitionLog() {
            return partitionLog;
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

        @Override
        public XLog getLog() {
            return getPartitionLog();
        }

        public boolean isChildOf(PartitionInfo partitionInfo){
            return resursiveChildern.contains(partitionInfo.getId());
        }

        public boolean isRalated(PartitionInfo partitionInfo){
            return this.isChildOf(partitionInfo) || partitionInfo.isChildOf(this);
        }

        public Set<UUID> getChildren() {
            return this.getAllRecursiveNodes(this.node).map(x->x.getID()).collect(Collectors.toSet());
        }

        public Node getNode(){
            return this.node;
        }


        //public String toString() {
        //    StringBuilder builder = new StringBuilder();
        //    toString(this.node, builder);
        //    return builder.toString();
        //}

        private void toString(Node node, StringBuilder builder) {
            if (node != null) {
                builder.append(node.toStringShort());
                if (node instanceof Block) {
                    Block block = (Block) node;
                    builder.append("(");
                    Iterator<Edge> it = block.getOutgoingEdges().iterator();
                    while (it.hasNext()) {
                        Edge edge = it.next();
                        builder.append(edge.getExpression());
                        PartitionInfo partitionInfo = getPartitions().get(edge.getTarget().getID());
                        if (partitionInfo != null){
                            String childTree = String.format("T_%d", partitionInfo.getSequentialId());
                            builder.append(childTree);
                        }
                        else {
                            toString(edge.getTarget(), builder);
                        }


                        //toString(edge.getTarget(), builder);
                        if (it.hasNext()) {
                            builder.append(", ");
                        }
                    }
                    builder.append(")");
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            String format = this.node.isRoot() ? "T_%d(Root)=" : "T_%d=";
            builder.append(String.format(format, this.getSequentialId()));
            toString(this.node, builder);

            Map<String, Long> s =
                    partitionLog.stream().map(x -> {
                        try {

                            //String[] activities = new String[trace.size()];
                            StringJoiner joiner = new StringJoiner(",");
                            for(int i = 0; i < x.size(); i++){
                                XEvent event = x.get(i);
                                String activity = event.getAttributes().get("concept:name").toString();
                                joiner.add(activity);
                            }

                            return String.format("<%s>", joiner.toString());

                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).filter(x->x != null).collect(
                            Collectors.groupingBy(
                                    Function.identity(), Collectors.counting()
                            )
                    );
            builder.append("; ");
            builder.append(String.format("L=%s", s));
            return builder.toString();

        }
        /*
        public void toString2(){

            if (this.getNode() instanceof AbstractBlock){
                AbstractBlock block = (AbstractBlock) this.getNode();
                for(Edge edge: block.getOutgoingEdges()){
                    String expression = edge.getExpression().getExpression();
                    UUID id = edge.getTarget().getID();
                    String s = getPartitions().get(id).getNode();
                }
            }

            return
        }*/

        public int getSequentialId(){
            return logIdentifiers.indexOf(this.getId());
        }
    }

    private ProcessTree processTree;
    private  LogHelper _helper;
    private HashMap<UUID, PartitionInfo> logs;
    private List<UUID> logIdentifiers;
    private IConformanceContext conformanceContext;
    private XLog origianlLog;

    public Partitioning(IConformanceContext conformanceContext, XLog log)
    {
        _helper = new LogHelper();
        this.processTree = new ProcessTreeImpl();
        logs = new HashMap<>();
        this.conformanceContext = conformanceContext;
        logIdentifiers = new ArrayList<>();
        this.origianlLog = log;
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
        if (!logIdentifiers.contains(node.getID())){
            logIdentifiers.add(node.getID());
        }
    }

    public ProcessTree getProcessTree() {
        return this.processTree;
    }

    public XLog getOrigianlLog(){
        return this.origianlLog;
    }
}