package org.eduprom.Miners.Synthesis.CutsMiners;

import org.eduprom.Miners.IMiner;
import org.eduprom.Miners.Synthesis.Entities.ProcessTreeCuts;
import org.deckfour.xes.model.XLog;

/**
 * Created by ydahari on 4/13/2017.
 */
public interface ICutsProcessTreeMiner extends IMiner {
    ProcessTreeCuts Mine(XLog log);
}
