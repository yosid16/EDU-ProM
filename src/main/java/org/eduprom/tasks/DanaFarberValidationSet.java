package org.eduprom.tasks;

import org.eduprom.miners.adaptiveNoise.benchmarks.AdaptiveNoiseBenchmark;
import org.eduprom.benchmarks.IBenchmark;
import org.eduprom.miners.adaptiveNoise.benchmarks.AdaptiveNoiseBenchmarkConfiguration;
import org.eduprom.benchmarks.configuration.NoiseThreshold;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class DanaFarberValidationSet {
	
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger logger = Logger.getLogger(DanaFarberValidationSet.class.getName());
	
    public static void main(String[] args) throws Exception {

		String trainFile = "EventLogs\\\\DFCI_Train_April.csv";
		//String testFile = "EventLogs\\\\DFCI_Test_May.csv";

		List<String> files = new ArrayList<>();
		files.add(trainFile);
		//files.add(testFile);
		//Float[] thresholds = new Float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f };
		//Float[] thresholds = new Float[] { 0.2f, 0.4f };
		//Float[] thresholds = new Float[] { 0.005f, 0.05f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f };


		logManager.readConfiguration(new FileInputStream("./app.properties"));
		logger.info("started application");

		try {
			AdaptiveNoiseBenchmarkConfiguration configuration = AdaptiveNoiseBenchmarkConfiguration.getBuilder()
					.useCrossValidation(false)
					.setNoiseThresholds(NoiseThreshold.uniform(0.1f))
					.addWeights()
					//.setPartitionNoiseFilter(0.0f)
					.build();
			IBenchmark benchmark = new AdaptiveNoiseBenchmark(configuration, 10);
			benchmark.run();

		} catch (Exception ex) {
			logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);
		}

		logger.info("ended application");
    }
}
