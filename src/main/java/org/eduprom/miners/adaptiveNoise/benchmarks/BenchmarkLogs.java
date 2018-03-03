package org.eduprom.miners.adaptiveNoise.benchmarks;

import org.deckfour.xes.model.XLog;

public class BenchmarkLogs {
    private XLog trainLog;

    private XLog validationLog;

    private XLog testLog;

    public XLog getTrainLog() {
        return trainLog;
    }

    public void setTrainLog(XLog trainLog) {
        this.trainLog = trainLog;
    }

    public XLog getValidationLog() {
        return validationLog;
    }

    public void setValidationLog(XLog validationLog) {
        this.validationLog = validationLog;
    }

    public XLog getTestLog() {
        return testLog;
    }

    public void setTestLog(XLog testLog) {
        this.testLog = testLog;
    }

    @Override
    public String toString() {
        return String.format("LOG - (TRAINING, VALIDATION, TEST): (%d, %d, %d)",
                trainLog.size(), getValidationLog().size(), getTestLog().size());
    }
}
