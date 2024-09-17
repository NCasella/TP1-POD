package ar.edu.itba.pod.server.exceptions;

public class DoctorNonExistentException extends RuntimeException{
    public DoctorNonExistentException(String msg){
        super(msg);
    }
}
