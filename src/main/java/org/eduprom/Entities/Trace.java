package org.eduprom.Entities;

import java.util.StringJoiner;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

/**
 * Created by ydahari on 22/10/2016.
 */
public class Trace {
	private XTrace _trace;
    public String FullTrace;
    public String[] Activities;

    public Trace(XTrace trace) throws Exception
    {
    	_trace = trace;
    	String[] activities = new String[trace.size()];
    	for(int i = 0; i < activities.length; i++){
    		XEvent event = trace.get(i);
    		String activity = event.getAttributes().get("concept:name").toString();
    		activities[i] = activity;    		
    	}
    	
        Activities = activities;
        StringJoiner joiner = new StringJoiner(" -> ");
        for(String a: activities){
            joiner.add(a);
        }
        FullTrace = joiner.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!Trace.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final Trace other = (Trace) obj;
        return this.FullTrace.equalsIgnoreCase(other.FullTrace);
    }

    @Override
    public int hashCode() {
        return FullTrace.hashCode();
    }
}
