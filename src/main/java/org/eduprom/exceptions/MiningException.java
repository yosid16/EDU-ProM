package org.eduprom.exceptions;

public class MiningException extends Exception {

    public MiningException(String message){
        super(message);
    }

    public MiningException(String message, Throwable ex){
        super(message, ex);
    }

    public MiningException(Throwable ex){
        super(ex);
    }
}
