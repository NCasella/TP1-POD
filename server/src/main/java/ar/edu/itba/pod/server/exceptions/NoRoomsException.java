package ar.edu.itba.pod.server.exceptions;

public class NoRoomsException extends RuntimeException{

    public NoRoomsException(){super("No rooms where created in the rooms repository");}
}
