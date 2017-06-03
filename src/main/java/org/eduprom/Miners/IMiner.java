package org.eduprom.Miners;

import org.eduprom.Entities.Trace;
import org.deckfour.xes.model.XLog;

import java.util.Iterator;

/**
 * Created by ydahari on 22/10/2016.
 */
public interface IMiner {
    String GetName();
    void Train();
    void Evaluate() throws Exception;
    void Export() throws Exception;
    XLog GetLog();
    Iterator<Trace> GetTraces();
}
