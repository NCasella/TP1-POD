package ar.edu.itba.pod.server.exceptions;

public class DoctorIsAttendingException extends RuntimeException{

    public DoctorIsAttendingException(String msg){
        super(msg);
    }
}
