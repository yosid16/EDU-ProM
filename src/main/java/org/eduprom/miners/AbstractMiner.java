package org.eduprom.miners;

import com.google.common.base.Stopwatch;
import org.apache.commons.io.FilenameUtils;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.exceptions.ParsingException;
import org.eduprom.utils.LogHelper;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.framework.packages.PackageManager.Canceller;
import org.deckfour.xes.model.XLog;

/***
 * First layer of miner abstraction.
 * Responsible for general mining tasks such as reading the event log etc.
 */
public abstract class AbstractMiner implements IMiner {
	
	protected static final Logger logger = Logger.getLogger(AbstractMiner.class.getName());

	//region private members

    private String name;
    private CLIContext promContext;
    private CLIPluginContext promPluginContext;

    //endregion

    //region protected members

    protected String filename;
    protected LogHelper logHelper;
    protected XLog log;
    protected Canceller canceller = ()-> false;
    private long elapsedMiliseconds;

    //endregion

    //region constructors

    public AbstractMiner(String filename) throws LogFileNotFoundException {
        name = getClass().getSimpleName();
        this.filename = filename;
        logHelper = new LogHelper();
        promContext = new org.processmining.contexts.cli.CLIContext();
        promPluginContext = new CLIPluginContext(promContext, getName());

        logHelper.checkFile(filename);
    }

    //endregion

    //region public methods

    @Override
    public void mine() {
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            logger.info(String.format("Started training the log file: %s using the algorithm: %s",
                    filename, getName()));
            readLog();
            //logHelper.printLogGrouped(Level.FINE, log);
            logger.info(String.format("reading event log finished successfully,log size: %s", log.size()));

            mineSpecific();
            logger.info(String.format("Training the log file: %s using the algorithm: %s has completed successfully"
                    , filename, getName()));
            stopwatch.stop();
            this.elapsedMiliseconds = stopwatch.elapsed(TimeUnit.MILLISECONDS);



        } catch (MiningException e) {
            String message = String.format("Training the log file: %s using the algorithm: %s has failed"
                    , filename, getName());
            logger.log(Level.SEVERE, message, e);
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

    protected void readLog() throws ParsingException {
        if (this.log == null){
            this.log = logHelper.read(filename);
        }
    }

    protected abstract void mineSpecific() throws MiningException;

    @Override
    public void setLog(XLog log) {
        this.log = log;
    }

    protected String getOutputPath(){
        return String.format("./Output/%s_%s" ,
                getName(),
                FilenameUtils.removeExtension(Paths.get(filename).getFileName().toString()));
    }

    protected XEventClassifier getClassifier(){
        return new XEventNameClassifier();
    }

    @Override
    public long getElapsedMiliseconds() {
        return elapsedMiliseconds;
    }

    //endregion
}