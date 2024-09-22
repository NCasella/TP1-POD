package ar.edu.itba.pod.server.exceptions;

public class InternalServerException extends RuntimeException{

    private static final String MESSAGE = "There's been an internal error in the server.";

    public InternalServerException(){
        super(MESSAGE);
    }

}
