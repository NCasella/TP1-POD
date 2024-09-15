package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.models.Appointment;

import java.util.List;

public class RoomsRepository {
    List<Long> availableRooms;    //order by id - desventaja: ids = peqs poco usados, xq saco siempre desde peek
    List<Appointment> unavailableRooms;

    public List<Appointment> getUnavailableRooms() {
        return unavailableRooms;
    }

    public List<Long> getAvailableRooms() {
        return availableRooms;
    }
}
/*
Consultar el estado actual de los consultorios, en orden ascendente por número,
indicando para cada uno su número, si está libre u ocupado,
y en caso de estar ocupado el paciente y el médico correspondiente.

// solo se agregan rooms

public RoomsRepository() {
    availableRooms = new HashMap<>()
}
<RoomId,Emergency>

// Emergency, orden en que se resolvieron
// doc, patient, room
*/