package org.eduprom.partitioning;

import org.deckfour.xes.model.XLog;
import org.eduprom.utils.LogHelper;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.ProcessTreeImpl;

import java.util.HashMap;
import java.util.Map;

public class Partitioning
{
    private ProcessTree processTree;
    private  LogHelper _helper;
    private HashMap<Node, XLog> logs;

    private boolean hasPartitioningChild(Node node){
        return (((AbstractBlock)node).getOutgoingEdges()).stream().anyMatch(x -> logs.containsKey(x.getTarget()));
    }


    public Partitioning()
    {
        _helper = new LogHelper();
        processTree = new ProcessTreeImpl();
        logs = new HashMap<Node, XLog>();
    }

    @Override
    public String toString() {
        return String.format("Partitioning has: %d sublogs", getLogs().size());
        /*
        String s = "";

        for(Map.Entry<Node, XLog> entry : getExclusiveLogs().entrySet()){
            s += "\nstart log for node - " + entry.getKey().toString() + ":\n";
            s += _helper.toString(entry.getValue());
            s += "\nend log for node - " + entry.getKey().toString() + ":\n";
        }
        return s;
        */
    }

    public HashMap<Node, XLog> getLogs() {
        return logs;
    }

    public HashMap<Node, XLog> getExclusiveLogs(){
        HashMap<Node, XLog> exclusiveLogs = new HashMap<>();
        for (Map.Entry<Node, XLog> entry : logs.entrySet()){
            exclusiveLogs.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Node, XLog> entry : logs.entrySet()){
            entry.getKey().getParents().stream().forEach(x -> exclusiveLogs.remove(x));
        }

        return exclusiveLogs;
    }

    public ProcessTree getProcessTree() {
        return processTree;
    }
}