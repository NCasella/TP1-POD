package ar.edu.itba.pod.server.exceptions;

public class DoctorNotFoundException extends RuntimeException{
    private static final String MESSAGE = "Doctor %s is not registered.";

    public DoctorNotFoundException(String name){
        super(MESSAGE.formatted(name));
    }

}
