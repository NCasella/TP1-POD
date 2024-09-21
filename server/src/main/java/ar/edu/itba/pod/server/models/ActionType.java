package ar.edu.itba.pod.server.models;

import ar.edu.itba.pod.grpc.Service;

public enum ActionType {
    REGISTER,
    AVAILABLE,
    UNAVAILABLE,
    STARTED_CARING(true),
    ENDED_CARING(true),
    UNREGISTER;

    private final boolean caring;

    ActionType() { caring=false; }

    ActionType(boolean caring) {
        this.caring = caring;
    }

    public boolean isAboutCaring() {
        return caring;
    }

    public Service.Action toGrpc(){
        return Service.Action.forNumber(this.ordinal()+1);
    }

    public static ActionType ofAvailabilty(Availability availability) {
        return availability.isAvailable() ? AVAILABLE : UNAVAILABLE;
    }
}
