package org.eduprom.tasks;

import org.eduprom.benchmarks.AdaptiveNoiseBenchmark;
import org.eduprom.benchmarks.IBenchmark;
import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner;
import org.eduprom.miners.adaptiveNoise.configuration.AdaptiveNoiseConfiguration;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class Benchmark {
	
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger logger = Logger.getLogger(Benchmark.class.getName());
	
    public static void main(String[] args) throws Exception {

		String[] formats =
				{
						"EventLogs\\contest_dataset\\training_log_%s.xes"//,
						//"EventLogs\\contest_2017\\log%s.xes"
				};
		Integer[] fileNumbers = new Integer[] { 1 };
		List<String> files = Arrays.stream(fileNumbers).flatMap(x-> Arrays.stream(formats)
				.map(f -> String.format(f, x))).collect(Collectors.toList());

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {
			AdaptiveNoiseConfiguration configuration = AdaptiveNoiseConfiguration.getBuilder()
					.setNoiseThresholds(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f)
					.setFitnessWeight(0.34)
					.setPrecisionWeight(0.33)
					.setGeneralizationWeight(0.34)
					.build();
			IBenchmark benchmark = new AdaptiveNoiseBenchmark(files, configuration);
			benchmark.run();

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);
        }
        
        logger.info("ended application");
    }
}
