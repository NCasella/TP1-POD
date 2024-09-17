package ar.edu.itba.pod.server.exceptions;


public class DoctorAlreadyRegisteredException extends RuntimeException{

    public DoctorAlreadyRegisteredException(String msg){
        super(msg);
    }
}
