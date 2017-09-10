package org.eduprom.miners.demo;


import org.deckfour.xes.model.XLog;
import org.eduprom.miners.InductiveMiner;
import org.processmining.log.algorithms.LowFrequencyFilterAlgorithm;
import org.processmining.log.parameters.LowFrequencyFilterParameters;

public class InductiveVariationDemo extends InductiveMiner {

    public InductiveVariationDemo(String filename) throws Exception {
        super(filename);
    }

    @Override
    protected void readLog() throws Exception {
        XLog log = logHelper.Read(filename);
        LowFrequencyFilterParameters params = new LowFrequencyFilterParameters(log);
        params.setThreshold(20);
        this.log = (new LowFrequencyFilterAlgorithm()).apply(getPromPluginContext(), log, params);
    }

    @Override
    public void evaluate() throws Exception {
        super.evaluate();
    }
}
