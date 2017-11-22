package org.eduprom;

import org.eduprom.benchmarks.configuration.NoiseThreshold;
import org.eduprom.miners.IMiner;
import org.eduprom.miners.InductiveMiner;
import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseExhaustive;
import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Main {
	
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger logger = Logger.getLogger(Main.class.getName());
	
    public static void main(String[] args) throws Exception {

		//String filename = "EventLogs\\log1.xes";
		String filename = "EventLogs\\contest_dataset\\training_log_4.xes";
        //String filename = "EventLogs\\test4.csv";

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {
			IMiner miner = new AdaptiveNoiseExhaustive(filename, NoiseThreshold.single(0.1f));

        	miner.mine();
        	miner.export();
        	miner.evaluate();

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);
        }
        
        logger.info("ended application");
    }
}
