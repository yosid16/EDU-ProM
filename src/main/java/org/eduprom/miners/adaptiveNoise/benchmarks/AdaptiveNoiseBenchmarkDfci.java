
package org.eduprom.miners.adaptiveNoise.benchmarks;

import org.deckfour.xes.model.XLog;
import org.eduprom.entities.CrossValidationPartition;
import org.eduprom.exceptions.ParsingException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdaptiveNoiseBenchmarkDfci extends AdaptiveNoiseBenchmark {

    private final String testFile;


    public AdaptiveNoiseBenchmarkDfci(String testFile, AdaptiveNoiseBenchmarkConfiguration adaptiveNoiseBenchmarkConfiguration, int testSize) {
        super(adaptiveNoiseBenchmarkConfiguration, testSize);
        this.testFile = testFile;
    }

    @Override
    protected BenchmarkLogs getBenchmarkLogs(String filename) throws ParsingException {
        XLog log = logHelper.read(filename);
        XLog testLog = logHelper.read(testFile);

        List<CrossValidationPartition> crossValidationPartitions =  this.logHelper.crossValidationSplit(log, testSize);
        CrossValidationPartition validationPartition = crossValidationPartitions.stream().findAny().get();
        XLog validationLog = validationPartition.getLog();
        XLog trainingLog = CrossValidationPartition.bind(crossValidationPartitions.stream()
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
