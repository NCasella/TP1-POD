package ar.edu.itba.pod.server.exceptions;

public class NoAppointmentsFinishedException extends RuntimeException{
    public NoAppointmentsFinishedException(){
        super("No appointments where finished at the time");
    }
}
