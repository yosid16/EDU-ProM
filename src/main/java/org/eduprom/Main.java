package org.eduprom;

import org.eduprom.benchmarks.configuration.Logs;
import org.eduprom.benchmarks.configuration.NoiseThreshold;
import org.eduprom.benchmarks.configuration.Weights;
import org.eduprom.miners.IMiner;
import org.eduprom.miners.InductiveMiner;
import org.eduprom.miners.adaptiveNoise.AdaMiner;
import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseExhaustive;
import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.miners.adaptiveNoise.configuration.AdaptiveNoiseConfiguration;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Main {
	
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger logger = Logger.getLogger(Main.class.getName());
	
    public static void main(String[] args) throws Exception {
		String dfciMay = "EventLogs\\\\DFCI_Test_May.csv";
		//String filename = "EventLogs\\log1.xes";
		//String filename = "EventLogs\\contest_dataset\\training_log_2.xes";
        //String filename = "EventLogs\\test4.csv";
		String filename = "EventLogs\\contest_2017\\log4.xes";

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {
			AdaptiveNoiseConfiguration adaptiveNoiseConfiguration = AdaptiveNoiseConfiguration.getBuilder()
					.setNoiseThresholds(NoiseThreshold.uniform(0.1f).getThresholds())
					.setWeights(Weights.getUniform())
					.setPreExecuteFilter(false)
					.build();
			IMiner miner = new AdaMiner(filename, adaptiveNoiseConfiguration);
        	miner.mine();
        	logger.info(String.format("ADA - "));
        	miner.export();

			for(Float f : NoiseThreshold.uniform(0.1f).getThresholds()){
				IMiner inductiveMiner = new NoiseInductiveMiner(filename, f, false);
				inductiveMiner.mine();
				inductiveMiner.evaluate();
			}


        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);
        }
        
        logger.info("ended application");
    }
}
