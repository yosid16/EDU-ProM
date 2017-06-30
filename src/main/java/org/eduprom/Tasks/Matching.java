package org.eduprom.Tasks;

import org.deckfour.xes.model.XLog;
import org.eduprom.Miners.IProcessTreeMiner;
import org.eduprom.Miners.InductiveMiner;
import org.eduprom.Miners.Matching.NaiveMatching;
import org.eduprom.Partitioning.ILogSplitter;
import org.eduprom.Partitioning.InductiveLogSplitting;
import org.eduprom.Utils.LogHelper;

import java.io.FileInputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class Matching {
	
	private static final LogManager logManager = LogManager.getLogManager();
	final static Logger logger = Logger.getLogger(Matching.class.getName());
	
    public static void main(String[] args) throws Exception {

		String filename = "EventLogs\\sample.xes";

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {

			ILogSplitter logSplitter = new InductiveLogSplitting();
			LogHelper helper = new LogHelper();
			XLog log = helper.Read(filename);
			org.eduprom.Partitioning.Partitioning pratitioning = logSplitter.split(log);
			NaiveMatching naiveMatching = new NaiveMatching();
			List<IProcessTreeMiner> miners = InductiveMiner.WithNoiseThresholds(filename, new float[] { (float)0.1, (float)0.2 })
					.stream().map(x->(IProcessTreeMiner)x).collect(Collectors.toList());

			naiveMatching.match(pratitioning, miners);


			logger.info(pratitioning.toString());

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);;
        }
        
        logger.info("ended application");
    }
}
