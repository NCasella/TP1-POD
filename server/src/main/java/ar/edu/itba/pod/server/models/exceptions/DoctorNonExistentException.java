package ar.edu.itba.pod.server.models.exceptions;

public class DoctorNonExistentException extends RuntimeException{
    public DoctorNonExistentException(String msg){
        super(msg);
    }
}
