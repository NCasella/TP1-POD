package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.models.Appointment;


import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class RoomRepository {
    private final List<Long> availableRooms;
    private final List<Appointment> unavailableRooms;
    private AtomicLong idCounter= new AtomicLong(1);

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

    public void setMaxRoomId(AtomicLong maxRoomId) {
        this.idCounter= maxRoomId;
    }

    public long addRoom(){
        long roomId=idCounter.getAndIncrement();
        availableRooms.add(roomId);
        return roomId;
    }

    private synchronized void getRoomsAvailability(List<Long> availableRoomsCopy, List<Long> unavailableRoomsCopy ) {
        availableRoomsCopy.addAll(availableRooms);
        unavailableRooms.forEach(r -> unavailableRoomsCopy.add(r.getRoomId()));
    }

    // todo: esto podria estar en Service, pero no se si es buena practica
    public List<Service.RoomFullInfo> getRoomsState() {
        List<Service.RoomFullInfo> sortedRooms = new ArrayList<>();
        List<Long> availableRoomsCopy = new ArrayList<>();
        List<Long> unavailableRoomsCopy = new ArrayList<>();

        getRoomsAvailability(availableRoomsCopy,unavailableRoomsCopy);

        availableRoomsCopy.sort(Comparator.naturalOrder());
        unavailableRoomsCopy.sort(Long::compareTo);

        Iterator<Long> availableRoomsIterator = availableRoomsCopy.iterator();
        Iterator<Long> unavailableRoomsIterator = unavailableRoomsCopy.iterator();
        Long availableRoomId, unavailableRoomId;
        Service.RoomFullInfo room;
        if ( availableRoomsIterator.hasNext() && unavailableRoomsIterator.hasNext()) {
            availableRoomId = availableRoomsIterator.next();
            unavailableRoomId = unavailableRoomsIterator.next();
            boolean available;
            long id;
            while (availableRoomsIterator.hasNext() && unavailableRoomsIterator.hasNext()) {

                if (availableRoomId < unavailableRoomId) {
                    id = availableRoomId;
                    availableRoomId = availableRoomsIterator.next();
                    available = true;
                } else {
                    id = unavailableRoomId;
                    unavailableRoomId = unavailableRoomsIterator.next();
                    available = false;
                };
                room = Service.RoomFullInfo.newBuilder().setAvailability(available)
                        .setId(id).build();
                sortedRooms.add(room);
            }
        }
        while (availableRoomsIterator.hasNext()) {
            availableRoomId = availableRoomsIterator.next();
            room = Service.RoomFullInfo.newBuilder().setAvailability(true)
                    .setId(availableRoomId).build();
            sortedRooms.add(room);
        }
        while (unavailableRoomsIterator.hasNext()) {
            unavailableRoomId = unavailableRoomsIterator.next();
            room = Service.RoomFullInfo.newBuilder().setAvailability(false)
            .setId(unavailableRoomId).build();
            sortedRooms.add(room);
        }

        return sortedRooms;
    }

}