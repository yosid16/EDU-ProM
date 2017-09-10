package org.eduprom.miners;

/**
 * Created by ydahari on 22/10/2016.
 */
public interface IMiner {
    String getName();
    void mine();
    void evaluate() throws Exception;
    void export() throws Exception;
}
