package org.eduprom.Miners;

import org.eduprom.Entities.Trace;
import org.eduprom.Utils.LogHelper;
import org.eduprom.Utils.TraceHelper;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.framework.packages.PackageManager.Canceller;
import org.deckfour.xes.model.XLog;

/**
 * Created by ydahari on 22/10/2016.
 */
public abstract class AbstractMiner implements IMiner {
	
	protected final static Logger logger = Logger.getLogger(AbstractMiner.class.getName());

    protected String _filename;
    protected boolean _trained;
    protected String _name;
    protected LogHelper _logHelper;
    protected TraceHelper _traceHelper;
    protected CLIContext _promContext;
    protected CLIPluginContext _promPluginContext; ;
    protected XLog _log;


	protected Canceller _canceller = new Canceller() {
		
		@Override
		public boolean isCancelled() {
			// This for passing the canceller to ProM interface
			// We don't cancel since we do not work interactively
			return false;
		}
	};

    protected void ReadLog() throws Exception{
        if (_log == null){
            _log = _logHelper.Read(_filename);
        }
    }


    public AbstractMiner(String filename) throws Exception
    {
        _trained = false;
        _name = getClass().getSimpleName();
        _filename = filename;
        _logHelper = new LogHelper();
        _traceHelper = new TraceHelper();
        _promContext = new org.processmining.contexts.cli.CLIContext();
        _promPluginContext = new CLIPluginContext(_promContext, GetName());

        _logHelper.CheckFile(filename);
    }

    @Override
    public String GetName() {
        return _name;
    }

    @Override
    public void Train() {    	
        try {
        	logger.info(String.format("Started training the log file: %s using the algorithm: %s", 
        			_filename, _name));
            ReadLog();
            _logHelper.PrintLogGrouped(Level.FINE, _log);
            logger.info("Read event log successfully");

			TrainSpecific();
			logger.info(String.format("Training the log file: %s using the algorithm: %s has completed successfully"
					, _filename, _name));
			_trained = true;
		} catch (Exception e) {
			logger.info(String.format("Training the log file: %s using the algorithm: %s has failed"
					, _filename, _name));
			e.printStackTrace();
			_trained = false;
		}        
    }

    public XLog GetLog(){
        return _log;
    }

    protected abstract void TrainSpecific() throws Exception;

    public abstract void Evaluate() throws Exception;

    public abstract void Export() throws Exception;

    @Override
    public Iterator<Trace> GetTraces(){
    	return _traceHelper.iterator();    	
    };


    
    public String GetOutputPath(){    	
    	String name = String.format("./Output/%s_%s.png" , GetName(), Paths.get(_filename).getFileName());
    	return name;
    }

    public XEventClassifier GetClassifier(){
        return new XEventNameClassifier();
    }
}

