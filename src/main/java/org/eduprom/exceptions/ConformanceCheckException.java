package org.eduprom.exceptions;

/**
 * Created by ydahari on 9/11/2017.
 */
public class ConformanceCheckException extends MiningException {
    public ConformanceCheckException(String message) {
        super(message);
    }

    public ConformanceCheckException(String message, Throwable ex) {
        super(message, ex);
    }

    public ConformanceCheckException(Throwable ex) {
        super(ex);
    }
}
