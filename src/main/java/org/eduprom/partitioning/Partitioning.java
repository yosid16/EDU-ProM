package org.eduprom.partitioning;

import org.deckfour.xes.model.XLog;
import org.eduprom.miners.adaptiveNoise.ConformanceInfo;
import org.eduprom.utils.LogHelper;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.ProcessTreeImpl;

import java.util.HashMap;
import java.util.UUID;

public class Partitioning
{
    public class PartitionInfo {
        private XLog log;
        private ProcessTree processTree;
        private ConformanceInfo conformanceInfo;

        public PartitionInfo(XLog log){
            this.log = log;
        }



        public ProcessTree getProcessTree() {
            return processTree;
        }

        public void setProcessTree(ProcessTree processTree) {
            this.processTree = processTree;
        }

        public XLog getLog() {
            return log;
        }

        public ConformanceInfo getConformanceInfo() {
            return conformanceInfo;
        }

        public void setConformanceInfo(ConformanceInfo conformanceInfo) {
            this.conformanceInfo = conformanceInfo;
        }
    }

    private ProcessTree processTree;
    private  LogHelper _helper;
    private HashMap<UUID, PartitionInfo> logs;

    private boolean hasPartitioningChild(Node node){
        return (((AbstractBlock)node).getOutgoingEdges()).stream().anyMatch(x -> logs.containsKey(x.getTarget()));
    }


    public Partitioning()
    {
        _helper = new LogHelper();
        this.processTree = new ProcessTreeImpl();
        logs = new HashMap<UUID, PartitionInfo>();
    }

    @Override
    public String toString() {
        return String.format("Partitioning has: %d sublogs", getPartitions().size());
    }

    public HashMap<UUID, PartitionInfo> getPartitions() {
        return logs;
    }

    public void add(UUID id, XLog log){
        this.logs.put(id, new PartitionInfo(log));
    }

    public ProcessTree getProcessTree() {
        return this.processTree;
    }
}