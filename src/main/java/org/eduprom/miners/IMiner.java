package org.eduprom.miners;

import org.eduprom.exceptions.ConformanceCheckException;
import org.eduprom.exceptions.ExportFailedException;
import org.eduprom.exceptions.MiningException;

public interface IMiner {
    /***
     * Get the name of the miner
     * @return Name of the miner (i.e Inductive miner, Alpha miner etc.)
     */
    String getName();

    /***
     * Performs the mining operation, of mapping an event log to a process model
     */
    void mine() throws MiningException;

    /***
     * Evaluates the process model quality. For example may print fitness, precision information.
     * @throws Exception
     */
    void evaluate() throws ConformanceCheckException;

    /***
     * Exports the process model (for example - serialize and persists the process model, or export an image)
     * @throws Exception
     */
    void export() throws ExportFailedException;
}
