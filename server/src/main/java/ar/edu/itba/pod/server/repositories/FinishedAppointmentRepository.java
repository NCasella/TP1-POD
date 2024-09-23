package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Appointment;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class FinishedAppointmentRepository {
    BlockingQueue<Appointment> finishedAppointments = new PriorityBlockingQueue<>(10, (a1, a2) -> a1.getFinishTime().compareTo(a2.getFinishTime()));

    public void addAppointment(Appointment appointment) throws InterruptedException {
        finishedAppointments.put(appointment);
    }

    public BlockingQueue<Appointment> getFinishedAppointments(){
        return new PriorityBlockingQueue<>(finishedAppointments);
    }
}
