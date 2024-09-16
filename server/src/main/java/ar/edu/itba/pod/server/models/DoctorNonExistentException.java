package ar.edu.itba.pod.server.models;

public class DoctorNonExistentException extends RuntimeException{
    public DoctorNonExistentException(String msg){
        super(msg);
    }
}
