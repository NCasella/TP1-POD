package ar.edu.itba.pod.server.models;

import java.util.Arrays;

public enum Availability {
    AVAILABLE(1,"Available"),
    UNAVAILABLE(2,"Unavailable"),
    ATTENDING(3,"Attending");

    private final int disponibilityNumber;
    private final String disponibilityString;

    Availability(int disponibilityNumber,String disponibilityString){
        this.disponibilityNumber=disponibilityNumber;
        this.disponibilityString=disponibilityString;
    }

    public String getDisponibilityString(){return this.disponibilityString;}
    public static Availability getDisponibilityFromNumber(int disponibilityNumber){
        return Arrays.stream(Availability.values())
                .filter((disponibility -> disponibility.disponibilityNumber==disponibilityNumber)).findAny().orElseThrow(IllegalArgumentException::new);
    }
    public int getDisponibilityNumber(){return this.disponibilityNumber;}

    public boolean isAvailable(){
        return this.disponibilityNumber==AVAILABLE.disponibilityNumber;
    }
}
