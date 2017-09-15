package org.eduprom.exceptions;

public class ParsingException extends MiningException {

    public ParsingException(String message){
        super(message);
    }

    public ParsingException(String message, Throwable ex){
        super(message, ex);
    }

    public ParsingException(Throwable ex){
        super(ex);
    }
}
