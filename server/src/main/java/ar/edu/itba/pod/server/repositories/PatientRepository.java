package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Patient;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Queue;

public class PatientRepository {
    private final sortByRiskLevelAndArrivalTime criteria = new sortByRiskLevelAndArrivalTime();
    private final Queue<Patient> waitingRoom = new PriorityBlockingQueue<>(10, criteria);

    public Queue<Patient> getWaitingRoom() {
        return waitingRoom;
    }

    //public boolean addPatientToWaitingRoom(Patient patient){
    //}

    private static class sortByRiskLevelAndArrivalTime implements Comparator<Patient>{
       @Override
       public int compare(Patient p1, Patient p2){
           if (p2.getPatientLevel().equals(p1.getPatientLevel())){
              return p1.getArrivalTime().compareTo(p2.getArrivalTime());
           }
           return p2.getPatientLevel().ordinal() - p1.getPatientLevel().ordinal();
       }
    }

}

