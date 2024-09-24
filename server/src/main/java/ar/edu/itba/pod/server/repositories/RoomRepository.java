package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AppointmentNotFoundException;
import ar.edu.itba.pod.server.models.Appointment;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.models.Patient;


import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class RoomRepository {
    private final List<Long> availableRooms;
    private final List<Appointment> unavailableRooms;
    private final AtomicLong idCounter= new AtomicLong(1);

    public RoomRepository() {
        this.availableRooms = new CopyOnWriteArrayList<>();
        //this.unavailableRooms = new PriorityBlockingQueue<>(10,(Long::compareTo));
        this.unavailableRooms =  new CopyOnWriteArrayList<>();
    }

    public List<Appointment> getUnavailableRooms() {
        return unavailableRooms;
    }

    public List<Long> getAvailableRooms() {
        return availableRooms;
    }

    public boolean isRoomAvailable(long roomId) {
        return availableRooms.contains(roomId);
    }

    public AtomicLong getMaxRoomId() {
        return idCounter;
    }


    public long addRoom(){
        long roomId=idCounter.getAndIncrement();
        availableRooms.add(roomId);
        return roomId;
    }

    public Appointment getAppointment(long roomId, Patient patient, Doctor doctor){
        final Appointment appointment = new Appointment(roomId,patient,doctor,null);
        int index = unavailableRooms.indexOf(appointment);
        if(index <0)
            throw new AppointmentNotFoundException();
        return unavailableRooms.get(index);
    }

    // necesito usar synchronized para que sea atomico el pasaje
    // sino puede desconocerse el estado de un room si queda entre medio del remove y el add
    public synchronized void startAppointment(Appointment appointment){
        availableRooms.remove(appointment.getRoomId());
        unavailableRooms.add(appointment);
    }

    public synchronized void finishAppointment(Appointment appointment){
        unavailableRooms.remove(appointment);
        availableRooms.add(appointment.getRoomId());
    }

    public synchronized List<Appointment> getRoomsState() {
        List<Appointment> allRooms= new ArrayList<>(availableRooms.stream()
                .map((aLong -> new Appointment(aLong,null,null,null))).toList());
        allRooms.addAll(unavailableRooms);
        allRooms.sort(Comparator.comparingLong(Appointment::getRoomId));
        return allRooms;
    }

}