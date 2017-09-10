package org.eduprom.miners;

import org.apache.commons.io.FilenameUtils;
import org.eduprom.utils.LogHelper;
import org.eduprom.utils.TraceHelper;
import java.nio.file.Paths;
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
	
	protected static final Logger logger = Logger.getLogger(AbstractMiner.class.getName());

    private String name;
    private TraceHelper traceHelper;
    private CLIContext promContext;
    private CLIPluginContext promPluginContext;
    private Canceller canceller = ()-> false;

    protected String filename;
    protected LogHelper logHelper;
    protected XLog log;

    //region constructors

    public AbstractMiner(String filename) throws Exception
    {
        name = getClass().getSimpleName();
        this.filename = filename;
        logHelper = new LogHelper();
        traceHelper = new TraceHelper();
        promContext = new org.processmining.contexts.cli.CLIContext();
        promPluginContext = new CLIPluginContext(promContext, getName());

        logHelper.CheckFile(filename);
    }

    //endregion

    //region public methods

    @Override
    public void mine() {
        try {
            logger.info(String.format("Started training the log file: %s using the algorithm: %s",
                    filename, getName()));
            readLog();
            logHelper.PrintLogGrouped(Level.FINE, log);
            logger.info("Read event log successfully");

            mineSpecific();
            logger.info(String.format("Training the log file: %s using the algorithm: %s has completed successfully"
                    , filename, getName()));
        } catch (Exception e) {
            logger.info(String.format("Training the log file: %s using the algorithm: %s has failed"
                    , filename, getName()));
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return name;
    }



    //endregion

    //region protected methods

    protected CLIContext getPromContext(){
        return promContext;
    }

    protected CLIPluginContext getPromPluginContext(){
        return promPluginContext;
    }

    protected Canceller getCanceller(){
        return canceller;
    }
    protected void readLog() throws Exception{
        if (this.log == null){
            this.log = logHelper.Read(filename);
        }
    }

    protected abstract void mineSpecific() throws Exception;

    protected String getOutputPath(){
        return String.format("./Output/%s_%s" ,
                getName(),
                FilenameUtils.removeExtension(Paths.get(filename).getFileName().toString()));
    }

    protected XEventClassifier getClassifier(){
        return new XEventNameClassifier();
    }

    //endregion
}

