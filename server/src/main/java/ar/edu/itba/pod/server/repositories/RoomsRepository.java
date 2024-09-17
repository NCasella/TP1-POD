package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Appointment;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;


public class RoomsRepository {
    private final List<Long> availableRooms= new CopyOnWriteArrayList<>();    //order by id - desventaja: ids = peqs poco usados, xq saco siempre desde peek
    private final List<Appointment> unavailableRooms= new CopyOnWriteArrayList<>();
    private AtomicLong idCounter= new AtomicLong(1);
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


}
