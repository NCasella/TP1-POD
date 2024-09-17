package ar.edu.itba.pod.server.models;

import java.util.Arrays;

public enum Disponibility {
    AVAILABLE(1),
    UNAVAILABLE(2),
    ATTENDING(3);

    private final int disponibilityNumber;
    Disponibility(int disponibilityNumber){
        this.disponibilityNumber=disponibilityNumber;
    }

    public static Disponibility getDisponibilityFromNumber(int disponibilityNumber){
        return Arrays.stream(Disponibility.values())
                .filter((disponibility -> disponibility.disponibilityNumber==disponibilityNumber)).findAny().orElseThrow(IllegalArgumentException::new);
    }
    public int getDisponibilityNumber(){return this.disponibilityNumber;}
}
