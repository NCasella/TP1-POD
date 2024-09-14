package ar.edu.itba.pod.server.models;

import java.time.LocalDateTime;
import java.util.Objects;

public class Patient {

   private final String name;
   private final Level level;
   private final LocalDateTime arrivalTime; //arrival time or id based on arrival time

   public Patient(String name, Level level, LocalDateTime arrivalTime){
       this.name = name;
       this.level = level;
       this.arrivalTime = arrivalTime;
   }

    public String getPatientName() {
        return name;
    }

    public Level getPatientLevel(){
       return level;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    @Override
    public boolean equals(Object object){
       if (this == object){
           return true;
       }
       if (!(object instanceof Patient patient)){
           return false;
       }
       return this.name.equals(patient.name);
    }

    @Override
    public int hashCode() {
       return Objects.hashCode(name);
    }
}
