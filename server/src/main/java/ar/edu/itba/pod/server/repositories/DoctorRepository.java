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

    public void addDoctor(String doctorName, Level level ){
        if ( null != doctorMap.putIfAbsent(doctorName,new Doctor(doctorName,level)) ){
            throw new DoctorAlreadyRegisteredException(doctorName);
        }
    }

    public Doctor getDoctor(String name){
        return Optional.ofNullable(doctorMap.get(name)).orElseThrow(()-> new DoctorNotFoundException(name));
    }

    public boolean hasDoctor(String name){
        return doctorMap.containsKey(name);
    }

    public ArrayList<Doctor> getAllDoctors(){
        return new ArrayList<>(doctorMap.values());
    }

    public ArrayList<Doctor> getAllDoctorsWithLock(){
        ArrayList<Doctor> doctorArray = getAllDoctors();
        for (Doctor doctor : doctorArray) {
            doctor.lockDoctor();
        }
        return doctorArray;
    }


}
