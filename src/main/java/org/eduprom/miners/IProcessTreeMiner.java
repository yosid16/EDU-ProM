package org.eduprom.miners;

import org.deckfour.xes.model.XLog;
import org.processmining.processtree.ProcessTree;

/**
 * Created by ydahari on 4/13/2017.
 */
public interface IProcessTreeMiner extends IMiner {
    ProcessTree mineProcessTree(XLog log) throws Exception;
}
