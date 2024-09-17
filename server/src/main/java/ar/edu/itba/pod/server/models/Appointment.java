package ar.edu.itba.pod.server.models;

import java.time.LocalDateTime;

public class Appointment {
    private long roomId;
    private Patient patient;
    private Doctor doctor;
    private LocalDateTime startTime;

    public Appointment(long roomId, Patient patient, Doctor doctor, LocalDateTime startTime){
        this.doctor = doctor;
        this.roomId = roomId;
        this.patient = patient;
        this.startTime = startTime;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public void setDoctorInAppointment(Doctor doctor) {
        this.doctor = doctor;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof Appointment a1 && a1.getRoomId() == this.roomId && a1.getDoctor().equals(this.doctor) && a1.getPatient().equals(this.patient));
    }

    @Override
    public int hashCode() {
        return (int) roomId * this.doctor.hashCode() * this.patient.hashCode();
    }
    public long getRoomId() {
        return roomId;
    }
}
