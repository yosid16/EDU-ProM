package org.eduprom.tasks;

import org.deckfour.xes.model.XLog;
import org.eduprom.partitioning.ILogSplitter;
import org.eduprom.partitioning.InductiveLogSplitting;
import org.eduprom.utils.LogHelper;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Partitioning {
	
	private static final LogManager logManager = LogManager.getLogManager();
	final static Logger logger = Logger.getLogger(Partitioning.class.getName());
	
    public static void main(String[] args) throws Exception {

		String filename = "EventLogs\\sample.xes";

    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {

			ILogSplitter logSplitter = new InductiveLogSplitting();
			LogHelper helper = new LogHelper();
			XLog log = helper.Read(filename);
			org.eduprom.partitioning.Partitioning pratitioning = logSplitter.split(log);
			logger.info(pratitioning.toString());

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the miner", ex);;
        }
        
        logger.info("ended application");
    }
}
