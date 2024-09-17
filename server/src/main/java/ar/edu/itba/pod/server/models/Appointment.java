package ar.edu.itba.pod.server.models;

public class Appointment {
    long roomId;
    Patient patient;
    Doctor doctor;

    public Appointment(long roomId, Patient patient, Doctor doctor){
        this.doctor = doctor;
        this.roomId = roomId;
        this.patient = patient;
    }
    public long getRoomId() {
        return roomId;
    }
}
