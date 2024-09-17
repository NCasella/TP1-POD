package ar.edu.itba.pod.server.models;

import java.util.Arrays;

public enum Disponibility {
    AVAILABLE(1,"Available"),
    UNAVAILABLE(2,"Unavailable"),
    ATTENDING(3,"Attending");

    private final int disponibilityNumber;
    private final String disponibilityString;

    Disponibility(int disponibilityNumber,String disponibilityString){
        this.disponibilityNumber=disponibilityNumber;
        this.disponibilityString=disponibilityString;
    }

    public String getDisponibilityString(){return this.disponibilityString;}
    public static Disponibility getDisponibilityFromNumber(int disponibilityNumber){
        return Arrays.stream(Disponibility.values())
                .filter((disponibility -> disponibility.disponibilityNumber==disponibilityNumber)).findAny().orElseThrow(IllegalArgumentException::new);
    }
    public int getDisponibilityNumber(){return this.disponibilityNumber;}
}
