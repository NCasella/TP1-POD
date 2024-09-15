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

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof Appointment a1 && a1.getRoomId() == this.roomId && a1.getDoctor().equals(this.doctor) && a1.getPatient().equals(this.patient));
    }

    @Override
    public int hashCode() {
        return (int) roomId * this.doctor.hashCode() * this.patient.hashCode();
    }
}
