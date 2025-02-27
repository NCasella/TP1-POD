package ar.edu.itba.pod.server.models;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Patient {

   private final String name;
   private Level level;
   private final LocalDateTime arrivalTime; //arrival time or id based on arrival time
   private final Lock  isModifiable = new ReentrantLock(true);
   public Patient(String name, Level level, LocalDateTime arrivalTime){
       this.name = name;
       this.level = level;
       this.arrivalTime = arrivalTime;
   }

    public Patient(String name) {
        this.name = name;
        this.arrivalTime = null;
    }

    public void lockPatient(){
       isModifiable.lock();
   }
   public void unlockPatient(){
       isModifiable.unlock();
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
    public void setLevel(Level level){
       isModifiable.lock();
       this.level=level;
       isModifiable.unlock();
   }
    @Override
    public int hashCode() {
       return Objects.hashCode(name);
    }
}
