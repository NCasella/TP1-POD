package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Disponibility;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.models.DoctorAlreadyRegisteredException;
import ar.edu.itba.pod.server.models.Level;
import ar.edu.itba.pod.server.models.DoctorNonExistentException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DoctorRepository {
    private final Map<String,Doctor> doctorMap=new ConcurrentHashMap<>();

    public synchronized void addDoctor(String doctorName, Level level ){
        if(doctorMap.containsKey(doctorName))
            throw new DoctorAlreadyRegisteredException(doctorName);
        doctorMap.put(doctorName, new Doctor(doctorName,level));
    }

    public Doctor getDoctor(String name){
        return Optional.ofNullable(doctorMap.get(name)).orElseThrow(()-> new DoctorNonExistentException(String.format("Doctor %s is not registered",name)));
    }

    public synchronized void setDoctorDisponibility(String name, Disponibility disponibility){
        doctorMap.get(name).setDisponibility(disponibility);
    }

    public synchronized void setDoctorLevel(String name,Level level){
        doctorMap.get(name).setLevel(level);
    }



}
