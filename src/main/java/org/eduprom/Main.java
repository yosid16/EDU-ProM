package org.eduprom;

import org.eduprom.Models.Alpha.AlphaPlus;
import org.eduprom.Models.IModel;
import org.eduprom.Models.InductiveMiner;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Main {
	
	private static final LogManager logManager = LogManager.getLogManager();
	final static Logger logger = Logger.getLogger(Main.class.getName());
	
    public static void main(String[] args) throws Exception {
    	
    	//String filename = "EventLogs\\contest_dataset\\test_log_may_1.xes";
    	String filename = "EventLogs\\sample.xes";
        //String filename = "EventLogs\\gamma2.csv";
    	logManager.readConfiguration(new FileInputStream("./app.properties"));
    	logger.info("started application");
    	    	    	
        try {
        	IModel model = new InductiveMiner(filename);
        	model.Train();
        	model.Export();
        	model.Evaluate();

        } catch (Exception ex) {
        	logger.log(Level.SEVERE, "exception when trying to train/evaluate the model", ex);;
        }
        
        logger.info("ended application");
    }
}
