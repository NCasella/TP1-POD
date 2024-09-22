package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.PatientAlreadyRegisteredException;
import ar.edu.itba.pod.server.exceptions.PatientNotInWaitingRoomException;
import ar.edu.itba.pod.server.models.Level;
import ar.edu.itba.pod.server.models.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Queue;

public class PatientRepository {
    private static final Logger LOGGER=LoggerFactory.getLogger(PatientRepository.class);
    private final sortByRiskLevelAndArrivalTime criteria = new sortByRiskLevelAndArrivalTime();
    private final Map<String,Patient> patientRegistry=new ConcurrentHashMap<>();
    private final BlockingQueue<Patient> waitingRoom = new PriorityBlockingQueue<>(10, criteria);

    public BlockingQueue<Patient> getWaitingRoom() {
        return waitingRoom;
    }
    public BlockingQueue<Patient> getWaitingRoomCopy() {
        return new PriorityBlockingQueue<>(waitingRoom);
    }
    public void setPatientLevel(String patientName,Level patientLevel) throws InterruptedException {
        //update
        //getPatientFromName(patientName).isModifiable() -> InterruptedException (lock)
        getPatientFromName(patientName).setLevel(patientLevel);
        waitingRoom.remove(new Patient(patientName, null, null));
        waitingRoom.put(getPatientFromName(patientName));
    }

    public synchronized void addpatient(String patientName, Level level) throws InterruptedException {
        if(patientRegistry.containsKey(patientName)){

            LOGGER.info("Throwing patientAlreadyRegistered exception");
            throw new PatientAlreadyRegisteredException(String.format("patient %s is already registered",patientName));
        }
        Patient patient = new Patient(patientName, level, LocalDateTime.now());
        patientRegistry.put(patientName,patient);
        waitingRoom.put(patient);
    }
    public Patient getPatientFromName(String patientName){
        return Optional.ofNullable(patientRegistry.get(patientName)).orElseThrow(()->new PatientNotInWaitingRoomException(String.format("Patient %s is not registered",patientName)));
    }

    public synchronized int getPatientsAhead(Patient patient) {
        if (!waitingRoom.contains(patient)){
            LOGGER.info("Throwing patienNotInWaitingRoom exception");
            throw new PatientNotInWaitingRoomException(String.format("patient %s not in waiting room", patient.getPatientName()));
        }
        int patientAt=waitingRoom.stream().toList().indexOf(patient);
        return patientAt;
    }

    private static class sortByRiskLevelAndArrivalTime implements Comparator<Patient>{
       @Override
       public int compare(Patient p1, Patient p2){
           if (p2.getPatientLevel().equals(p1.getPatientLevel())){
              return p1.getArrivalTime().compareTo(p2.getArrivalTime());
           }
           return p2.getPatientLevel().getLevelNumber() - p1.getPatientLevel().getLevelNumber();
       }
    }

}

