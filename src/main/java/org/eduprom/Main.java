package org.eduprom;

import org.eduprom.miners.EnumeratePaths;
import org.eduprom.miners.IMiner;
import org.eduprom.miners.InductiveMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.miners.adaptiveNoise.RecursiveScan;
import org.eduprom.miners.adaptiveNoise.TestTreeChangesMiner;
import org.eduprom.utils.LogHelper;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Main {
	
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger logger = Logger.getLogger(Main.class.getName());
	
    public static void main(String[] args) throws Exception {

		//String filename = "EventLogs\\log1.xes";
		String filename = "EventLogs\\contest_dataset\\test_log_may_4.xes";
        //String filename = "EventLogs\\test.csv";

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {

			IMiner miner = new RecursiveScan(filename, 0.5, 0.5,
					0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f);
        	miner.mine();
        	//miner.export();
        	//miner.evaluate();

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);
        }
        
        logger.info("ended application");
    }
}
