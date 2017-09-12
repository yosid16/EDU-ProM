package org.eduprom.utils;

import java.util.HashMap;
import java.util.Map;

import org.eduprom.entities.Trace;

public class TraceHelper  {
		
	private Map<Trace, Integer> traces;
	
	public TraceHelper(){
		traces = new HashMap<>();
	}
	
	/**
	 * Saves the current trace and the number of cumulative occurrences ().
	 * @param t A trace to add. 
	 */
	synchronized public void Add(Trace t){
		Integer value = traces.putIfAbsent(t, 1);
		if(value != null){
			value++;
		}		
	}
	
	public void Clear(){
		traces.clear();
	}
	
	
	public boolean Exists(Trace t){
		return traces.containsKey(t);
	}
}
