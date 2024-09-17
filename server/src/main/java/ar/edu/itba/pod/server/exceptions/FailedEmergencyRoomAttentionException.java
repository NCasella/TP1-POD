package ar.edu.itba.pod.server.exceptions;

public abstract class FailedEmergencyRoomAttentionException extends RuntimeException{
    private static final String MESSAGE = "Room #%d remains Free";
    private long roomId;
    public FailedEmergencyRoomAttentionException(long roomId){
        super();
        this.roomId = roomId;
    }

    @Override
    public String toString(){
        return String.format(MESSAGE, roomId);
    }
}
