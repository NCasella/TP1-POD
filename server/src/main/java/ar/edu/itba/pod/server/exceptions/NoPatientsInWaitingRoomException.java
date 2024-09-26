package ar.edu.itba.pod.server.exceptions;

public class NoPatientsInWaitingRoomException extends FailedEmergencyRoomAttentionException {
    private final static String MESSAGE = "\nThere are no patients in the Waiting Room";
    public NoPatientsInWaitingRoomException(long roomId) {
        super(MESSAGE, roomId);
    }
}
