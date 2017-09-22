package org.eduprom.tasks;

import org.eduprom.miners.IMiner;
import org.eduprom.miners.InductiveMiner;
import org.eduprom.miners.adaptiveNoise.RecursiveScan;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class LogSeries {
	
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger logger = Logger.getLogger(LogSeries.class.getName());
	
    public static void main(String[] args) throws Exception {

		//String filenameFormat = "EventLogs\\contest_2017\\log%s.xes";
		String filenameFormat = "EventLogs\\contest_dataset\\test_log_may_%s.xes";
		Integer[] fileNumbers = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }; //
		List<String> files = Arrays.stream(fileNumbers).map(x -> String.format(filenameFormat, x)).collect(Collectors.toList());

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {

        	for(String filename : files){
				IMiner miner = new RecursiveScan(filename, 0.0f, 0.2f, 0.4f);
				miner.mine();
				miner.export();
			}

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);
        }
        
        logger.info("ended application");
    }
}
