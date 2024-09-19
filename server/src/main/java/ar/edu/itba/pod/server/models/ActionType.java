package ar.edu.itba.pod.server.models;

import ar.edu.itba.pod.grpc.Service;

public enum ActionType {
    REGISTER(false),
    AVAILABILITY(false),
    STARTED_CARING(true),
    ENDED_CARING(true),
    UNREGISTER(false);

    private final boolean caring;

    ActionType(boolean caring) {
        this.caring = caring;
    }

    public boolean isAboutCaring() {
        return caring;
    }

    public Service.Action toGrpc(){
        return Service.Action.forNumber(this.ordinal()+1);
    }
}
