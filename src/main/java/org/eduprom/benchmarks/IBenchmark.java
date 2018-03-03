package org.eduprom.benchmarks;

import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.miners.adaptiveNoise.AdaptiveNoiseMiner;

import java.util.List;

public interface IBenchmark<T, U> {

    /***
     *
     * @return the new algorithm
     */
    List<T> getSources(String filename) throws Exception;

    /***
     *
     * @return previous algorithms to compare with
     */
    List<U> getTargets(String filename) throws LogFileNotFoundException;


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
