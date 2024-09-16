package ar.edu.itba.pod.server.models;

public class DoctorIsAttendingException extends RuntimeException{

    public DoctorIsAttendingException(String msg){
        super(msg);
    }
}
