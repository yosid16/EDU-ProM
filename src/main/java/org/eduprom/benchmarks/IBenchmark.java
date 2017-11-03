package org.eduprom.benchmarks;

import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.miners.IMiner;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

import java.util.List;

public interface IBenchmark {

    /***
     *
     * @return the new algorithm
     */
    List<IBenchmarkableMiner> getSources(String filename) throws Exception;

    /***
     *
     * @return previous algorithms to compare with
     */
    List<IBenchmarkableMiner> getTargets(String filename) throws LogFileNotFoundException;


    /***
     * Benchmark name
     * @return
     */
    String getName();

    /***
     * Runs the benchmark
     */
    void run() throws Exception;
}
