package ar.edu.itba.pod.models;

import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.grpc.Service.Action;

import java.util.Map;
import java.util.Objects;


public enum ActionType {
    REGISTER("Doctor %s (%d) registered successfully for pager"),
    AVAILABLE("Doctor %s (%d) is Available"),
    UNAVAILABLE("Doctor %s (%d) is Unavailable"),
    STARTED_CARING("Patient %s (%d) and Doctor %s (%d) are now in Room #%d"){
        @Override
        public String toString(String name, Service.Notification notification) {
            return STARTED_CARING.MESSAGE.formatted(notification.getPatient(), notification.getPatientLevel().getNumber(),name,notification.getDoctorLevel().getNumber(),notification.getRoomId());
        }
    },
    ENDED_CARING("Patient %s (%d) has been discharged from Doctor %s (%d) and the Room #%d is now Free") {
        @Override
        public String toString(String name, Service.Notification notification) {
            return ENDED_CARING.MESSAGE.formatted(notification.getPatient(), notification.getPatientLevel().getNumber(),name,notification.getDoctorLevel().getNumber(),notification.getRoomId());
        }
    },
    UNREGISTER("Doctor %s (%d) unregistered successfully for pager");

    private final String MESSAGE;
    private final static ActionType[] values = ActionType.values();

    ActionType(String MESSAGE) {
        this.MESSAGE = MESSAGE;
    }

    public static ActionType getAction(Service.Notification notification){
        return values[notification.getActionValue()-1];
    }


    public String toString(String name, Service.Notification notification) {
        return MESSAGE.formatted(name,notification.getDoctorLevel().getNumber());
}
}
