package org.eduprom.miners;

/**
 * Created by ydahari on 22/10/2016.
 */
public interface IMiner {
    /***
     * Get the name of the miner
     * @return Name of the miner (i.e Inductive miner, Alpha miner etc.)
     */
    String getName();

    /***
     * Performs the mining operation, of mapping an event log to a process model
     */
    void mine();

    /***
     * Evaluates the process model quality. For example may print fitness, precision information.
     * @throws Exception
     */
    void evaluate() throws Exception;

    /***
     * Exports the process model (for example - serialize and persists the process model, or export an image)
     * @throws Exception
     */
    void export() throws Exception;
}
