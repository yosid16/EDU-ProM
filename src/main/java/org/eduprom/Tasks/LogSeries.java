package org.eduprom.Tasks;

import org.eduprom.Miners.IMiner;
import org.eduprom.Miners.InductiveMiner;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class LogSeries {
	
	private static final LogManager logManager = LogManager.getLogManager();
	final static Logger logger = Logger.getLogger(LogSeries.class.getName());
	
    public static void main(String[] args) throws Exception {

		String filenameFormat = "EventLogs\\contest_2017\\log%s.xes";
		Integer[] fileNumbers = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		List<String> files = Arrays.stream(fileNumbers).map(x -> String.format(filenameFormat, x)).collect(Collectors.toList());

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {

        	for(String filename : files){
				IMiner miner = new InductiveMiner(filename);
				miner.Train();
				miner.Export();
				miner.Evaluate();
			}
        	/*
        	IMiner miner = new ConformanceTraversal(filename,
					new InductiveCutMiner(filename),
					InductiveMiner.WithNoiseThreshold(filename, (float)0.1),
					InductiveMiner.WithNoiseThreshold(filename, (float)0.2),
					InductiveMiner.WithNoiseThreshold(filename, (float)0.3),
					InductiveMiner.WithNoiseThreshold(filename, (float)0.4));
        	*/

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);;
        }
        
        logger.info("ended application");
    }
}
