package org.eduprom.exceptions;

public class ProcessTreeConversionException extends MiningException {

    public ProcessTreeConversionException(String message){
        super(message);
    }

    public ProcessTreeConversionException(String message, Throwable ex){
        super(message, ex);
    }

    public ProcessTreeConversionException(Throwable ex){
        super(ex);
    }
}
