package ar.edu.itba.pod.server.exceptions;

public class PatientNotInWaitingRoomException extends RuntimeException{
private final static String MESSAGE = "patient %s not in waiting room";
public PatientNotInWaitingRoomException(String patientName){
    super(MESSAGE.formatted(patientName));
}
}
