package org.eduprom.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eduprom.entities.Trace;
import org.apache.commons.io.FilenameUtils;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.model.XLog;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.ParsingException;
import org.processmining.log.csv.CSVFileReferenceOpenCSVImpl;
import org.processmining.log.csv.config.CSVConfig;
import org.processmining.log.csvimport.CSVConversion.ConversionResult;
import org.processmining.log.csvimport.config.CSVConversionConfig;
import org.processmining.log.csvimport.exception.CSVConversionException;


public class LogHelper {

	private static final Logger logger = Logger.getLogger(LogHelper.class.getName());
	/**
	 * 
	 * @param filename A valid full/relative path to a file
	 * @return indication if the file exists
	 * @throws LogFileNotFoundException in case the log file cannot be found in the path specified
	 */
	public void checkFile(String filename) throws LogFileNotFoundException {
		
		Path path = Paths.get(filename);
		if (Files.notExists(path)) {
			throw new LogFileNotFoundException(String.format("File does not exists, path: %s", path));
		}
	}
	
	
	/**
	 * Loads a csv file to an in-memory object compatible with ProM algorithms
	 * @param filename A valid full/relative path to a file
	 * @return In-memory object compatible with ProM algorithms
	 * @throws ParsingException In case that the log file cannot be parsed
	 */
    public XLog readCsv(String filename) throws ParsingException {
    	try{
			Path path = Paths.get(filename);
			CSVFileReferenceOpenCSVImpl csvFile = new org.processmining.log.csv.CSVFileReferenceOpenCSVImpl(path);
			CSVConfig cf = new CSVConfig();
			CSVConversionConfig config = new CSVConversionConfig(csvFile, cf);
			config.autoDetect();
			ConversionResult<XLog> cr = new org.processmining.log.csvimport.CSVConversion().doConvertCSVToXES(csvFile, cf, config);
			if (cr.hasConversionErrors()){
				throw new CSVConversionException(String.format("Conversion failed: %s", cr.getConversionErrors()));
			}

			return cr.getResult();
		}
		catch (Exception ex){
    		throw new ParsingException(ex);
		}
    }
    
    /**
     * Loads a xes file to an in-memory object compatible with ProM algorithms
     * 
     * @param filename A valid full/relative path to a file
     * @return In-memory object compatible with ProM algorithms
     * @throws Exception In cases where parsing failed
     */
    public XLog readXes(String filename) throws ParsingException {
    	XUniversalParser uParser = new org.deckfour.xes.in.XUniversalParser();
    	File file = new File(filename);
    	if (!uParser.canParse(file))
    	{
    		throw new ParsingException("the given file could not be parsed");
    	}

		Collection<XLog> logs;
    	try{
			logs = uParser.parse(file);
		}
		catch (Exception ex){
    		throw new ParsingException(ex);
		}

    	if (logs.size() > 1){
    		throw new ParsingException("the xes format contains multiple logs");
    	}
    	
    	return logs.iterator().next();
    }    
    
       
    /**
     * Loads a csv/xes file to an in-memory object compatible with ProM algorithms
     * @param filename A valid full/relative path to a file
     * @return In-memory object compatible with ProM algorithms
     * @throws Exception In cases where parsing failed
     */
    public XLog read(String filename) throws ParsingException {
    	String extention = FilenameUtils.getExtension(filename);    	
    	
    	if (extention.equalsIgnoreCase("csv")){
    		return readCsv(filename);
    	}    	
    	else if (extention.equalsIgnoreCase("xes")){
    		return readXes(filename);
    	
    	} else {
    		throw new ParsingException("the given file extention isn't supported");
    	}
    }

	public String toString(XLog log){
		return log.stream().map(x -> {
			try {
				return new Trace(x).FullTrace;
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Failure to read trace", e);
				return null;
			}
		}).filter(x->x != null).collect (Collectors.joining (","));
	}

	public void printLog(Level level, XLog log){
		String s = toString(log);
		logger.log(level, String.format("Log: %s", s));
	}

	public void printLogGrouped(Level level, XLog log){

		Map<String, Long> s =
				log.stream().map(x -> {
					try {
						return new Trace(x).FullTrace;
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}).filter(x->x != null).collect(
						Collectors.groupingBy(
								Function.identity(), Collectors.counting()
						)
				);
		//s.entrySet().stream().filter(x->x.getValue() > 1).collect(Collectors.toList())
		//s.entrySet().stream().sorted(Map.Entry::getValue).forEach(x -> logger.info(x.toString()));
		logger.log(level, String.format("Log: %s", s));
	}
}
