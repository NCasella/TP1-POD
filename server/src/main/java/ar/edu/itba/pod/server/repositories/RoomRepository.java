package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Appointment;


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

    public AtomicLong getMaxRoomId() {
        return idCounter;
    }


    public long addRoom(){
        long roomId=idCounter.getAndIncrement();
        availableRooms.add(roomId);
        return roomId;
    }



    public synchronized List<Appointment> getRoomsState() {
        List<Appointment> allRooms= new ArrayList<>(availableRooms.stream()
                .map((aLong -> new Appointment(aLong,null,null,null))).toList());
        allRooms.addAll(unavailableRooms);
        allRooms.sort(Comparator.comparingLong(Appointment::getRoomId));
        return allRooms;
    }

}