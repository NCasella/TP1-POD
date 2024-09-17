package ar.edu.itba.pod.server.exceptions;

public class AppointmentNotFoundException extends RuntimeException{
    private static final String MESSAGE = "There is no room with that doctor attending.";
    @Override
    public String toString(){
        return MESSAGE;
    }

}
