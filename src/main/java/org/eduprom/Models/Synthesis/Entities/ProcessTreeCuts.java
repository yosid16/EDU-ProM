package org.eduprom.Models.Synthesis.Entities;

import org.deckfour.xes.model.XLog;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.ProcessTreeImpl;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by ydahari on 4/13/2017.
 */
public class ProcessTreeCuts
{
    public ProcessTree processTree;

    public HashMap<UUID, XLog> logs;

    public ProcessTreeCuts()
    {
        processTree = new ProcessTreeImpl();
        logs = new HashMap<UUID, XLog>();
    }
}