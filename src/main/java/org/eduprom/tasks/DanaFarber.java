package org.eduprom.tasks;

import org.eduprom.benchmarks.configuration.Logs;
import org.eduprom.benchmarks.configuration.NoiseThreshold;
import org.eduprom.benchmarks.configuration.Weights;
import org.eduprom.miners.adaptiveNoise.benchmarks.AdaptiveNoiseBenchmarkConfiguration;
import org.eduprom.miners.adaptiveNoise.benchmarks.AdaptiveNoiseBenchmarkDfci;
import org.eduprom.benchmarks.IBenchmark;
import org.eduprom.partitioning.InductiveCutSplitting;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class DanaFarber {
	
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger logger = Logger.getLogger(DanaFarber.class.getName());
	
    public static void main(String[] args) throws Exception {

		String trainFile = "EventLogs\\\\DFCI_Train_April.csv";
		String testFile = "EventLogs\\\\DFCI_Test_May.csv";

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {

			AdaptiveNoiseBenchmarkConfiguration configuration = AdaptiveNoiseBenchmarkConfiguration.getBuilder()
					.useCrossValidation(false)
					.setNoiseThresholds(NoiseThreshold.uniform(0.05f))
					//.addWeights(Weights.getUniform())
					.addLogs(Logs.getBuilder().addFile(trainFile).build())
					.addWeights(Weights.getRangePrecision(0.1))
					//.addWeights(new Weights(0.0, 0.0, 1.0))
					.setPreExecuteFilter(false)
					.setLogSplitter(InductiveCutSplitting.class) //InductiveCutSplitting
					.build();
			IBenchmark benchmark = new AdaptiveNoiseBenchmarkDfci(testFile, configuration, 10);
			//IBenchmark benchmark = new AdaptiveNoiseBenchmark(new ArrayList<String>() {{ add(trainFile); add(testFile); }},
			//		configuration, 10);
			benchmark.run();

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);
        }
        
        logger.info("ended application");
    }
}
