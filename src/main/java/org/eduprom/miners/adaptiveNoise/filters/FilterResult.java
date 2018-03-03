package org.eduprom.miners.adaptiveNoise.filters;

import org.deckfour.xes.model.XLog;

public class FilterResult
{
    private XLog filteredLog;
    private int bitsRemoved;
    private int bits;

    public FilterResult(XLog filteredLog, int bitsRemoved, int bits){
        this.filteredLog = filteredLog;
        this.bitsRemoved = bitsRemoved;
        this.bits = bits;
    }

    public XLog getFilteredLog() {
        return filteredLog;
    }

    public int getBitsRemoved() {
        return bitsRemoved;
    }

    public int getBits(){
        return this.bits;
    }
}