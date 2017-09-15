package org.eduprom.miners.synthesis.cutsMiners;

import org.eduprom.miners.IMiner;
import org.eduprom.miners.synthesis.entities.ProcessTreeCuts;
import org.deckfour.xes.model.XLog;

public interface ICutsProcessTreeMiner extends IMiner {
    ProcessTreeCuts mineCutProcessTree(XLog log);
}
