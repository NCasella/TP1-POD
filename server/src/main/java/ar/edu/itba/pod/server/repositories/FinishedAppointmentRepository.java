package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Appointment;
import ar.edu.itba.pod.server.models.Patient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class FinishedAppointmentRepository {
    private final Comparator<Appointment> criteria = (a1, a2) -> a1.getFinishTime().compareTo(a2.getFinishTime());
    BlockingQueue<Appointment> finishedAppointments = new PriorityBlockingQueue<>(10, criteria);

    public void addAppointment(Appointment appointment) throws InterruptedException {
        finishedAppointments.put(appointment);
    }

    // no se quitan elementos => no precisa de un synchronized, no existe riesgo
    public List<Appointment> getFinishedAppointmentsList(){
        final BlockingQueue<Appointment> finishedAppointmentsCopy = new PriorityBlockingQueue<>(finishedAppointments);
        final List<Appointment> list = new ArrayList<>();
        finishedAppointmentsCopy.drainTo(list);
        return list;
    }
}
