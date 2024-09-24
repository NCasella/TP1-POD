package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.PatientAlreadyRegisteredException;
import ar.edu.itba.pod.server.exceptions.PatientNotInWaitingRoomException;
import ar.edu.itba.pod.server.models.Level;
import ar.edu.itba.pod.server.models.Pair;
import ar.edu.itba.pod.server.models.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class PatientRepository {
    private final sortByRiskLevelAndArrivalTime criteria = new sortByRiskLevelAndArrivalTime();
    private final Map<String,Patient> patientRegistry=new ConcurrentHashMap<>();
    private final BlockingQueue<Patient> waitingRoom = new PriorityBlockingQueue<>(10, criteria);

    public BlockingQueue<Patient> getWaitingRoom() {
        return waitingRoom;
    }
    public BlockingQueue<Patient> getWaitingRoomCopy() {
        return new PriorityBlockingQueue<>(waitingRoom);
    }

    public void setPatientLevel(Patient patient,Level patientLevel) throws InterruptedException {
        patient.lockPatient();
        try {
            if( !waitingRoom.contains(patient))                                         // dentro del lock asi me aseguro de que no esta en proceso de seleccion de carePatients
                throw new PatientNotInWaitingRoomException(patient.getPatientName());   // sino se volveria a meter a un paciente ya atendido al waiting room!, y con otro level

            patient.setLevel(patientLevel);
            synchronized (this) {
                waitingRoom.remove(new Patient(patient.getPatientName(), null, null)); // si no es atomico el remove y el put, 2 threads pueden poner 2 veces un paciente
                waitingRoom.put(patient);
            }
        } finally {
            patient.unlockPatient();
        }
    }

    public synchronized void addpatient(String patientName, Level level) throws InterruptedException {
        if(patientRegistry.containsKey(patientName)){
            throw new PatientAlreadyRegisteredException(String.format("patient %s is already registered",patientName));
        }
        Patient patient = new Patient(patientName, level, LocalDateTime.now());
        patientRegistry.put(patientName,patient);
        waitingRoom.put(patient);
    }
    public Patient getPatientFromName(String patientName){
        return Optional.ofNullable(patientRegistry.get(patientName)).orElseThrow(()->new PatientNotInWaitingRoomException(String.format("Patient %s is not registered",patientName)));
    }

    /* retorna level del momento en que se obtuvo el indexOf */
    public Pair<Integer,Level> getPatientsAhead(Patient patient) {
        // me pueden cambiar el level del paciente -> debo usar su lock
        int patientAt;

        if (waitingRoom.contains(patient)){
            patient.lockPatient();
            try {                                                                           // patienAt >=0 por si entre el contains() y el lock() lo quitan de la queue
                if ((patientAt =getWaitingRoomList().indexOf(patient)) >= 0)                //      -> tal vez esta operacion se haga demas, pero es muy poco probable
                    return new Pair<>(patientAt,patient.getPatientLevel());                 //      y evito poner contains() adentro del lock
            } finally {
                patient.unlockPatient();
            }
        }
        throw new PatientNotInWaitingRoomException(patient.getPatientName());
    }

    public synchronized ArrayList<Patient> getWaitingRoomList() {       // synchronized asi evito que esten tocando la queue
        Comparator<? super Patient> criteria= new sortByRiskLevelAndArrivalTime();
        ArrayList<Patient> patientList = new ArrayList<>(waitingRoom);
        patientList.sort(criteria);
        return patientList;
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

