package org.eduprom.Models.Synthesis.CutsMiners;

import org.eduprom.Models.IModel;
import org.eduprom.Models.Synthesis.Entities.ProcessTreeCuts;
import org.deckfour.xes.model.XLog;

/**
 * Created by ydahari on 4/13/2017.
 */
public interface ICutsProcessTreeMiner extends IModel {
    ProcessTreeCuts Mine(XLog log);
}
