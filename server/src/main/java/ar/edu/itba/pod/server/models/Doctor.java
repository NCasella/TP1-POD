package ar.edu.itba.pod.server.models;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Doctor {
    private final String doctorName;
    private Level level;
    private Availability disponibility;
    private final Lock isModifiable = new ReentrantLock(true);

    public Doctor(String doctorName, Level level) {
        this.doctorName = doctorName;
        this.level = level;
        this.disponibility=Availability.AVAILABLE;
    }

    public void lockDoctor(){
        isModifiable.lock();
    }

    public void unlockDoctor(){
        isModifiable.unlock();
    }
    public String getDoctorName() {
        return doctorName;
    }
    public void setDisponibility(Availability disponibility){
        isModifiable.lock();
        this.disponibility=disponibility;
        isModifiable.unlock();
    }
    public Availability getDisponibility(){
//        isModifiable.lock();
//        isModifiable.unlock();
        return this.disponibility;
    }

    public Level getLevel() {
//        isModifiable.lock();
//        isModifiable.unlock();
        return level;
    }

    public void setLevel(Level level) {
        isModifiable.lock();
        this.level = level;
        isModifiable.unlock();
    }

    @Override
    public boolean equals(Object anObject){
        if(this==anObject)
            return true;
        if(!(anObject instanceof Doctor doctor))
            return false;
        return this.doctorName.equals(doctor.doctorName);
    }
    @Override
    public int hashCode(){
        return Objects.hashCode(doctorName);
  }
}
