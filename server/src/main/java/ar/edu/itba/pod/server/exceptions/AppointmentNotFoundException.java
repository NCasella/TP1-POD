package ar.edu.itba.pod.server.exceptions;

import ar.edu.itba.pod.server.models.Appointment;

public class AppointmentNotFoundException extends RuntimeException{
    private static final String MESSAGE = "The doctor and patient described were not found in the room provided.";
    public AppointmentNotFoundException(){
        super(MESSAGE);
    }

}
