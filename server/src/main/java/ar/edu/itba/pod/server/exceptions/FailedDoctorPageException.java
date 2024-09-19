package ar.edu.itba.pod.server.exceptions;

public class FailedDoctorPageException extends RuntimeException{
    private static final String MESSAGE = "Notification couldn't be delivered";
    public FailedDoctorPageException(){
        super(MESSAGE);
    }

}
