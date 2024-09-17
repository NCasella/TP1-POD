package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.DoctorNotFoundException;

import ar.edu.itba.pod.server.models.Disponibility;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.models.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DoctorRepository {
    private final Map<String, Doctor> doctorMap= new HashMap<>();
    

    public synchronized void addDoctor(String doctorName, Level level ){//ver de si se reciben los parametros o si recibe el objeto ya creado desde el server
        if(!doctorMap.containsKey(doctorName))
            doctorMap.put(doctorName, new Doctor(doctorName,level));
        //todo tirar excepciones aca dependiendo del caso?
    }
    /*
    public Doctor getDoctor(String name){
        return doctorMap.get(name);
    }*/

    public Optional<Doctor> getDoctor(String name) {
        return Optional.ofNullable(doctorMap.get(name));
    }

    public synchronized void setDoctorDisponibility(String name, Disponibility disponibility){
        doctorMap.get(name).setDisponibility(disponibility);
    }
    public synchronized void setDoctorLevel(String name,Level level){
        doctorMap.get(name).setLevel(level);
    }



}
