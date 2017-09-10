package org.eduprom.miners.synthesis.CutsMiners;

import org.eduprom.miners.IMiner;
import org.eduprom.miners.synthesis.entities.ProcessTreeCuts;
import org.deckfour.xes.model.XLog;

/**
 * Created by ydahari on 4/13/2017.
 */
public interface ICutsProcessTreeMiner extends IMiner {
    ProcessTreeCuts Mine(XLog log);
}
