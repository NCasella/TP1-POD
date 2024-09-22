package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.QueryMakerGrpc;
import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.models.Appointment;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.repositories.PatientRepository;
import ar.edu.itba.pod.server.repositories.RoomRepository;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QueryServiceimpl extends QueryMakerGrpc.QueryMakerImplBase {

    private final RoomRepository roomRepository;
    private final PatientRepository patientRepository;

    public QueryServiceimpl(RoomRepository roomRepository, PatientRepository patientRepository){
        this.roomRepository=roomRepository;
        this.patientRepository=patientRepository;
    }

    @Override
    public void queryCares(Service.Query request, StreamObserver<Service.FinishedAppointmentsState> responseObserver) {
        super.queryCares(request, responseObserver);
    }

    @Override
    public void queryWaitingRooms(Empty request, StreamObserver<Service.PatientsCurrentState> responseObserver) {

    }

    @Override
    public void queryRooms(Empty request, StreamObserver<Service.RoomsCurrentState> responseObserver) {
        List<Appointment> roomList=roomRepository.getRoomsState();
        List<Service.RoomFullInfo> roomsInfoList= roomList.stream().map((appointment) -> {
            Service.RoomFullInfo.Builder roomInfoBuilder= Service.RoomFullInfo.newBuilder().setAvailability(false);
            Doctor doctor = appointment.getDoctor();
            if(doctor !=null){
                roomInfoBuilder.setAvailability(true);
                roomInfoBuilder.setRoomInfo(Service.RoomBasicInfo.newBuilder().setDoctor(doctor.getDoctorName())
                        .setDoctorLevel(Service.Level.forNumber(doctor.getLevel().getLevelNumber()))
                        .setPatient(appointment.getPatient().getPatientName()).setPatientLevel(Service.Level.forNumber(appointment.getPatient().getPatientLevel().getLevelNumber()))
                        .build());
            }
            return roomInfoBuilder.setId(appointment.getRoomId()).build();
        }).toList();
        responseObserver.onNext(Service.RoomsCurrentState.newBuilder().addAllRooms(roomsInfoList).build());
        responseObserver.onCompleted();
    }
}
