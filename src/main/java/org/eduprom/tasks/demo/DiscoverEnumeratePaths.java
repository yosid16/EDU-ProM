package org.eduprom.tasks.demo;

import org.eduprom.miners.IMiner;
import org.eduprom.miners.demo.EnumeratePathsDemo;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class DiscoverEnumeratePaths {
	
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger logger = Logger.getLogger(DiscoverEnumeratePaths.class.getName());
	
    public static void main(String[] args) throws Exception {

        String filename = "EventLogs\\sample.xes";

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {
        	IMiner miner = new EnumeratePathsDemo(filename);
        	miner.mine();
        	miner.export();
        	miner.evaluate();

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);
        }
        
        logger.info("ended application");
    }
}
