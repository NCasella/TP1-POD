package ar.edu.itba.pod.server.exceptions;

public class NoDoctorsAvailableException extends FailedEmergencyRoomAttentionException{
    private static final String MESSAGE = "\nThere are no doctors available to attend the patients waiting.";
    public NoDoctorsAvailableException(long roomId){
        super(MESSAGE, roomId);
    }

    @Override
    public String toString(){
        StringBuilder messageError = new StringBuilder(super.toString()).append(MESSAGE);
        return messageError.toString();
    }
}
