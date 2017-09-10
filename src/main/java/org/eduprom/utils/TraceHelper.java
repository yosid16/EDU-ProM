package org.eduprom.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.eduprom.entities.Trace;

public class TraceHelper  {
		
	public HashMap<Trace, Integer> Traces;
	
	public TraceHelper(){
		Traces = new HashMap<Trace, Integer>();		
	}
	
	/**
	 * Saves the current trace and the number of cumulative occurrences ().
	 * @param t A trace to add. 
	 */
	synchronized public void Add(Trace t){
		Integer value = Traces.putIfAbsent(t, 1);
		if(value != null){
			value++;
		}		
	}
	
	public void Clear(){
		Traces.clear();
	}
	
	
	public boolean Exists(Trace t){
		return Traces.containsKey(t);
	}
	
	public Iterator<Trace> iterator() {
		
        Iterator<Trace> iprof = Traces.keySet().stream()
        		.flatMap(x -> Collections.nCopies(1, x).stream())
        		.iterator();
        		
        return iprof;
    }

}
