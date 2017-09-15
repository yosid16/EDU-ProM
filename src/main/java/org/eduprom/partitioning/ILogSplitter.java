package org.eduprom.partitioning;

import org.deckfour.xes.model.XLog;

public interface ILogSplitter {

    Partitioning split(XLog log);
}
