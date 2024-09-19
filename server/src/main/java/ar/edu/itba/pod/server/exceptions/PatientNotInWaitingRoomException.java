package ar.edu.itba.pod.server.exceptions;

public class PatientNotInWaitingRoomException extends RuntimeException{

public PatientNotInWaitingRoomException(String msg){
    super(msg);
}
}
