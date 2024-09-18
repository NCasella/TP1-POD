package ar.edu.itba.pod.server.exceptions;

//esta podria no extender de la que refiere a la room vacia
//pero del enunciado no me queda claro si todas las causas de fallas tienen que necesariamente mostrar el mismo mensaje de base
public class RoomIdNotFoundException extends FailedEmergencyRoomAttentionException{

    private static final String MESSAGE = "\nThere is no room with that number.";
    public RoomIdNotFoundException(long roomId){
        super(MESSAGE, roomId);
    }

    @Override
    public String toString(){
        StringBuilder messageError = new StringBuilder(super.toString()).append(MESSAGE);
        return messageError.toString();
    }

}
