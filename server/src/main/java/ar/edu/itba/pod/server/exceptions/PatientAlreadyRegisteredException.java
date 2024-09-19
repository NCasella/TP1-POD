package ar.edu.itba.pod.server.exceptions;

public class PatientAlreadyRegisteredException extends RuntimeException {
    public PatientAlreadyRegisteredException(String msg){
        super(msg);
    }
}
