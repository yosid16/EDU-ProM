package org.eduprom.tasks;

import org.eduprom.benchmarks.configuration.Logs;
import org.eduprom.benchmarks.configuration.NoiseThreshold;
import org.eduprom.benchmarks.configuration.Weights;
import org.eduprom.miners.adaptiveNoise.benchmarks.AdaBenchmark;
import org.eduprom.miners.adaptiveNoise.benchmarks.AdaptiveNoiseBenchmark;
import org.eduprom.benchmarks.IBenchmark;
import org.eduprom.miners.adaptiveNoise.benchmarks.AdaptiveNoiseBenchmarkConfiguration;
import org.eduprom.partitioning.InductiveCutSplitting;
import org.eduprom.partitioning.trunk.InductiveLogSplitting;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class Benchmark {
	
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger logger = Logger.getLogger(Benchmark.class.getName());
	public static final String dfciApril = "EventLogs\\\\DFCI_Train_April.csv";
	public static final String dfciMay = "EventLogs\\\\DFCI_Test_May.csv";
	
    public static void main(String[] args) throws Exception {

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {
			AdaptiveNoiseBenchmarkConfiguration configuration = AdaptiveNoiseBenchmarkConfiguration.getBuilder()
					.addLogs(Logs.getBuilder().addNumbers(1).addFormat(Logs.CONTEST_2017).build())
					//.setLogSplitter(InductiveLogSplitting.class)
					//.addLogs(Logs.getBuilder().addNumbers(1, 10).addFormat(dfciApril).build())
					//.setNoiseThresholds(NoiseThreshold.uniform(0.02f))
					.addWeights(new Weights(0.2, 0.4, 0.4))
					//.addWeights(Weights.getRange(0.1))
					//.setPartitionNoiseFilter(0.2f)
					//.addWeights(new Weights(0.2, 0.4, 0.4))
					.build();
			IBenchmark benchmark = new AdaBenchmark(configuration, 10);
			benchmark.run();

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);
        }
        
        logger.info("ended application");
    }
}
