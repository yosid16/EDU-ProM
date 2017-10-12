package org.eduprom.tasks;

import org.eduprom.miners.adaptiveNoise.RecursiveScan;
import org.eduprom.miners.IMiner;

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
            //IMiner miner = new RecursiveScan(filename, 0.0f, 0.1f, 0.2f, 0.3f);
            //miner.mine();
            //miner.export();

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);
        }
        
        logger.info("ended application");
    }
}
