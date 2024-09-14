package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Appointment;

import java.util.List;

public class AppointmentRepository {
    List<Appointment> appointments;

    public void addAppointment(Appointment appointment) {
        appointments.add(appointment);
    }
}
