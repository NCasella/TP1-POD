package ar.edu.itba.pod.server.exceptions;


public class DoctorAlreadyRegisteredException extends RuntimeException{
    private static final String MESSAGE = "Doctor %s is already registered in the system.";

    public DoctorAlreadyRegisteredException(String msg){
        super(MESSAGE.formatted(msg));
    }
}
