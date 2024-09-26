package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AppointmentNotFoundException;
import ar.edu.itba.pod.server.exceptions.NoRoomsException;
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

    public boolean hasRooms(){
        return getMaxRoomId()>0;
    }

    public long getMaxRoomId() {
        return idCounter.get()-1;
    }

    public long addRoom(){
        long roomId=idCounter.getAndIncrement();
        availableRooms.add(roomId);
        return roomId;
    }

    // synchronized para garantizar que la operacion sea thread-safe
    public synchronized Appointment getAppointment(long roomId, Patient patient, Doctor doctor){
        return unavailableRooms.stream().filter(a -> a.getRoomId() == roomId && a.getDoctor().equals(doctor) && a.getPatient().equals(patient)).findAny()
                .orElseThrow(AppointmentNotFoundException::new);
    }

    // synchronized para que sea atomico el pasaje
    // sino puede desconocerse el estado de un room si queda entre medio del remove y el add
    // si se desconoce el estado -> getRoomsState() no lo va a incluir!
    public synchronized void startAppointment(Appointment appointment){
        if (availableRooms.remove(appointment.getRoomId()))
            unavailableRooms.add(appointment);
    }

    public synchronized void finishAppointment(Appointment appointment){
        if (unavailableRooms.remove(appointment))
            availableRooms.add(appointment.getRoomId());
    }

    // synchronized garantiza que no falten rooms o haya repetidos (estan en las 2 listas o en ninguna)
    public synchronized List<Appointment> getRoomsState() {
        if(availableRooms.isEmpty() && unavailableRooms.isEmpty()){
            throw new NoRoomsException();
        }
        List<Appointment> allRooms= new ArrayList<>(availableRooms.stream()
                .map((aLong -> new Appointment(aLong,null,null,null))).toList());
        allRooms.addAll(unavailableRooms);
        return allRooms;
    }

}