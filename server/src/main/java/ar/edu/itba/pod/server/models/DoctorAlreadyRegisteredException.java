package ar.edu.itba.pod.server.models;

public class DoctorAlreadyRegisteredException extends RuntimeException{

    public DoctorAlreadyRegisteredException(String msg){
        super(msg);
    }
}
