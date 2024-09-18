package ar.edu.itba.pod.server.exceptions;

public abstract class FailedEmergencyRoomAttentionException extends RuntimeException{
    private static final String MESSAGE = "Room #%d remains Free";
    public FailedEmergencyRoomAttentionException(String message, long roomId){
        super(MESSAGE.formatted(roomId) + message);
    }

}
