package ar.edu.itba.pod.server.models;

public class Appointment {
    private long roomId;
    private Patient patient;
    private Doctor doctor;

    public Appointment(long roomId, Patient patient, Doctor doctor){
        this.doctor = doctor;
        this.roomId = roomId;
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public long getRoomId() {
        return roomId;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }
}
