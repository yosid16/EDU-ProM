package org.eduprom.entities;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CrossValidationPartition {
    private XLog log;

    public CrossValidationPartition(List<XTrace> traces, XAttributeMap attributeMap){
        this.log = new XLogImpl(attributeMap);
        this.log.addAll(traces);
    }

    public XLog getLog() {
        return log;
    }

    public static CrossValidationPartition bind(List<CrossValidationPartition> partitions){

        List<XTrace> traces = partitions.stream()
                .flatMap(x -> x.getLog().stream()).collect(Collectors.toList());
        return new CrossValidationPartition(traces, partitions.stream().findAny().get().getLog().getAttributes());
    }

    public static CrossValidationPartition bind(CrossValidationPartition... partitions){
        return CrossValidationPartition.bind(Arrays.stream(partitions).collect(Collectors.toList()));
    }

    public static List<CrossValidationPartition> exclude(List<CrossValidationPartition> origin,  CrossValidationPartition... excluded){
        return origin.stream().filter(x-> Arrays.stream(excluded).allMatch(y -> y != x)).collect(Collectors.toList());
    }

    public static CrossValidationPartition[] take(List<CrossValidationPartition> origin,  int num){
        return origin.stream().limit(num).toArray(CrossValidationPartition[]::new);
    }
}
