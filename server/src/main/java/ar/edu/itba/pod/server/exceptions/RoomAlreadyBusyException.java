package ar.edu.itba.pod.server.exceptions;

public class RoomAlreadyBusyException extends FailedEmergencyRoomAttentionException{
    private static final String MESSAGE = "\nThe room is already in use.";
    public RoomAlreadyBusyException(long roomId){
        super(MESSAGE, roomId);
    }

    @Override
    public String toString(){
        StringBuilder messageError = new StringBuilder(super.toString()).append(MESSAGE);
        return messageError.toString();
    }

}
