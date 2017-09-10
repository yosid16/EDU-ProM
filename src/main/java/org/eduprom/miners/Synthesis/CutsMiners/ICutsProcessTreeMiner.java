package org.eduprom.miners.Synthesis.CutsMiners;

import org.eduprom.miners.IMiner;
import org.eduprom.miners.Synthesis.Entities.ProcessTreeCuts;
import org.deckfour.xes.model.XLog;

/**
 * Created by ydahari on 4/13/2017.
 */
public interface ICutsProcessTreeMiner extends IMiner {
    ProcessTreeCuts Mine(XLog log);
}
