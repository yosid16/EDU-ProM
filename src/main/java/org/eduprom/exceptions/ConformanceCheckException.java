package org.eduprom.exceptions;

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
