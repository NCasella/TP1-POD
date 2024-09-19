package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.DoctorNotFoundException;
import ar.edu.itba.pod.server.models.Availability;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.exceptions.DoctorAlreadyRegisteredException;
import ar.edu.itba.pod.server.models.Level;
import java.util.ArrayList;
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
        return Optional.ofNullable(doctorMap.get(name)).orElseThrow(()-> new DoctorNotFoundException(name));
    }

    public ArrayList<Doctor> getAllDoctors(){
        return new ArrayList<>(doctorMap.values());
    }

    public synchronized void setDoctorDisponibility(String name, Availability availability){
        doctorMap.get(name).setDisponibility(availability);
    }

    public synchronized void setDoctorLevel(String name,Level level){
        doctorMap.get(name).setLevel(level);
    }



}
