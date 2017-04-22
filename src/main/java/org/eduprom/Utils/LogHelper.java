package org.eduprom.Utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eduprom.Entities.Trace;
import org.apache.commons.io.FilenameUtils;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.model.XLog;
import org.processmining.log.csv.CSVFileReferenceOpenCSVImpl;
import org.processmining.log.csv.config.CSVConfig;
import org.processmining.log.csvimport.CSVConversion.ConversionResult;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.exception.CSVConversionConfigException;
import org.processmining.log.csvimport.exception.CSVConversionException;


public class LogHelper {
	
	final static Logger logger = Logger.getLogger(LogHelper.class.getName());
	/**
	 * 
	 * @param filename A valid full/relative path to a file
	 * @return indication if the file exists
	 * @throws Exception 
	 */
	public void CheckFile(String filename) throws Exception{
		
		Path path = Paths.get(filename);
		if (Files.notExists(path)) {
			throw new Exception("File does not exists");
		}
				
		//File file = new File(filename);
		//if (!file.isDirectory())
		//   file = file.getParentFile();
		//if (!file.exists()){
		//    throw new Exception("File doe not exists");
		//}
	}
	
	
	/**
	 * Loads a csv file to an in-memory object compatible with ProM algorithms
	 * @param filename A valid full/relative path to a file
	 * @return In-memory object compatible with ProM algorithms
	 * @throws CSVConversionConfigException In cases where configuration is not valid
	 * @throws CSVConversionException In cases where conversion failed
	 */
    public XLog ReadCsv(String filename) throws CSVConversionConfigException, CSVConversionException
    {
		Path path = Paths.get(filename);
		CSVFileReferenceOpenCSVImpl csvFile = new org.processmining.log.csv.CSVFileReferenceOpenCSVImpl(path);
		CSVConfig cf = new CSVConfig();
		CSVConversionConfig config = new CSVConversionConfig(csvFile, cf);
		config.autoDetect();
		ConversionResult<XLog> cr = new org.processmining.log.csvimport.CSVConversion().doConvertCSVToXES(csvFile, cf, config);
		if (cr.hasConversionErrors()){
			throw new CSVConversionException("Conversion failed: {0}".format(cr.getConversionErrors()));
		}
		
		return cr.getResult();
    }
    
    /**
     * Loads a xes file to an in-memory object compatible with ProM algorithms
     * 
     * @param filename A valid full/relative path to a file
     * @return In-memory object compatible with ProM algorithms
     * @throws Exception In cases where parsing failed
     */
    public XLog ReadXes(String filename) throws Exception
    {
    	XUniversalParser uParser = new org.deckfour.xes.in.XUniversalParser();
    	File file = new File(filename);
    	if (!uParser.canParse(file))
    	{
    		throw new Exception("the given file could not be parsed");
    	}
    	Collection<XLog> logs = uParser.parse(file);
    	
    	if (logs.size() > 1){
    		throw new Exception("the xes format contains multiple logs");
    	}
    	
    	return logs.iterator().next();
    }    
    
       
    /**
     * Loads a csv/xes file to an in-memory object compatible with ProM algorithms
     * @param filename A valid full/relative path to a file
     * @return In-memory object compatible with ProM algorithms
     * @throws Exception In cases where parsing failed
     */
    public XLog Read(String filename) throws Exception
    {
    	String extention = FilenameUtils.getExtension(filename);    	
    	
    	if (extention.equalsIgnoreCase("csv")){
    		return ReadCsv(filename);    		
    	}    	
    	else if (extention.equalsIgnoreCase("xes")){
    		return ReadXes(filename);
    	
    	} else {
    		throw new Exception("the given file extention isn't supported");
    	}
    }

	public void PrintLog(Level level, XLog log){
		String s = log.stream().map(x -> {
			try {
				return new Trace(x).FullTrace;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}).filter(x->x != null).collect (Collectors.joining (","));

		logger.log(level, String.format("Log: %s", s));
	}
}
