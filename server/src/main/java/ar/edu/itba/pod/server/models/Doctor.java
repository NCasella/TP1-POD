package ar.edu.itba.pod.server.models;

import java.util.Objects;

public class Doctor {
    private final String doctorName;
    private Level level;
    private Disponibility disponibility;

    public Doctor(String doctorName, Level level) {
        this.doctorName = doctorName;
        this.level = level;
        this.disponibility=Disponibility.AVAILABLE;
    }

    public String getDoctorName() {
        return doctorName;
    }
    public void setDisponibility(Disponibility disponibility){
        this.disponibility=disponibility;
    }
    public Disponibility getDisponibility(){
        return this.disponibility;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
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
