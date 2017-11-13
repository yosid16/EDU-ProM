package org.eduprom.miners.adaptiveNoise.IntermediateMiners;

import org.eduprom.miners.adaptiveNoise.filters.FilterResult;
import org.processmining.processtree.ProcessTree;

public class MiningResult {

    private ProcessTree processTree;
    private FilterResult filterResult;

    public MiningResult(ProcessTree processTree, FilterResult filterResult){
        this.processTree = processTree;
        this.filterResult = filterResult;
    }

    public ProcessTree getProcessTree() {
        return processTree;
    }

    public FilterResult getFilterResult() {
        return filterResult;
    }
}
