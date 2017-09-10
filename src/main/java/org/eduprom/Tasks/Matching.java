package org.eduprom.Tasks;

import org.eduprom.Miners.AdaptiveNoise.RecursiveScan;
import org.eduprom.Miners.IMiner;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Matching {
	
	private static final LogManager logManager = LogManager.getLogManager();
	final static Logger logger = Logger.getLogger(Matching.class.getName());
	
    public static void main(String[] args) throws Exception {

		String filename = "EventLogs\\sample.xes";

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {
            IMiner miner = new RecursiveScan(filename, 0.0f, 0.1f, 0.2f, 0.3f);
            miner.Train();
            miner.Export();

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);
        }
        
        logger.info("ended application");
    }
}
