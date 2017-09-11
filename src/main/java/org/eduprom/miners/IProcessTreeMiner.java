package org.eduprom.miners;

import org.deckfour.xes.model.XLog;
import org.processmining.processtree.ProcessTree;

public interface IProcessTreeMiner extends IMiner {
    ProcessTree mineProcessTree(XLog log) throws Exception;
}
