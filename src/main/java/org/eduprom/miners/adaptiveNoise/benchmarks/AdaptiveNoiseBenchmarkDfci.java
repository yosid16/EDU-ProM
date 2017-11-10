
package org.eduprom.miners.adaptiveNoise.benchmarks;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.deckfour.xes.model.XLog;
import org.eduprom.benchmarks.IBenchmark;
import org.eduprom.benchmarks.IBenchmarkableMiner;
import org.eduprom.benchmarks.Weights;
import org.eduprom.entities.CrossValidationPartition;
import org.eduprom.exceptions.ConformanceCheckException;
import org.eduprom.exceptions.ExportFailedException;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.ParsingException;
import org.eduprom.miners.AbstractMiner;
import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner;
import org.eduprom.miners.adaptiveNoise.ConformanceInfo;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.miners.adaptiveNoise.TreeChanges;
import org.eduprom.utils.LogHelper;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.processmining.plugins.petrinet.replayresult.PNRepResult.TRACEFITNESS;

public class AdaptiveNoiseBenchmarkDfci extends AdaptiveNoiseBenchmark {

    private final String testFile;


    public AdaptiveNoiseBenchmarkDfci(String trainFile, String testFile, AdaptiveNoiseBenchmarkConfiguration adaptiveNoiseBenchmarkConfiguration, int testSize) {
        super(new ArrayList<String>() {{ add(trainFile); }}, adaptiveNoiseBenchmarkConfiguration, testSize);
        this.testFile = testFile;
    }

    @Override
    protected BenchmarkLogs getBenchmarkLogs(String filename) throws ParsingException {
        XLog log = logHelper.read(filename);
        XLog testLog = logHelper.read(testFile);

        List<CrossValidationPartition> crossValidationPartitions =  this.logHelper.crossValidationSplit(log, testSize);
        CrossValidationPartition validationPartition = crossValidationPartitions.stream().findAny().get();
        XLog validationLog = validationPartition.getLog();
        XLog trainingLog = CrossValidationPartition.Bind(crossValidationPartitions.stream()
                .filter(x -> x != validationPartition)
                .collect(Collectors.toList())).getLog();
        return new BenchmarkLogs()
        {{
            setTrainLog(trainingLog);
            setValidationLog(validationLog);
            setTestLog(testLog);
        }};


    }
}
