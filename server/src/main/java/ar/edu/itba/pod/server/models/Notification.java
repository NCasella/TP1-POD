package ar.edu.itba.pod.server.models;

import ar.edu.itba.pod.grpc.Service;

import java.util.Objects;
import java.util.concurrent.Semaphore;

public class Notification {
    private final Level patientLevel;
    private final Level doctorLevel;
    private final ActionType actionType;
    private final String patient;
    private final long roomId;

    public Notification(Level doctorLevel, ActionType actionType, String patient
            ,Level patientLevel, long roomId) {
        this.patientLevel = patientLevel;
        this.doctorLevel = doctorLevel;
        this.actionType = actionType;
        this.patient = patient;
        this.roomId = roomId;
    }

    public Notification(Level doctorLevel, ActionType actionType) {
        this.doctorLevel = doctorLevel;
        this.actionType = actionType;
        this.patient = null;
        this.roomId = -1;
        this.patientLevel = null;
    }

    public boolean isUnregistered(){
        return ActionType.UNREGISTER.equals(actionType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if(!(o instanceof Notification n))
            return false;
        return actionType == n.actionType && (!actionType.isAboutCaring()
                || ( Objects.equals(patient, n.patient) &&
                patientLevel == n.patientLevel && doctorLevel == n.doctorLevel && roomId == n.roomId ));
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionType,patient,patientLevel,doctorLevel,roomId);
    }

    // transform grpc
    public Service.Notification toGrpc(){
        if (actionType.isAboutCaring())
            return Service.Notification.newBuilder().setDoctorLevel(doctorLevel.toGrpc()).setAction(actionType.toGrpc())
                .setPatient(patient).setPatientLevel(patientLevel.toGrpc()).setRoomId(roomId).build();
        return Service.Notification.newBuilder().setDoctorLevel(doctorLevel.toGrpc()).setAction(actionType.toGrpc()).build();
    }
}
