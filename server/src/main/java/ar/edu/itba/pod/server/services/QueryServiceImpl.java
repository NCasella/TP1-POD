package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.QueryMakerGrpc;
import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.models.Appointment;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.models.Patient;
import ar.edu.itba.pod.server.repositories.FinishedAppointmentRepository;
import ar.edu.itba.pod.server.repositories.PatientRepository;
import ar.edu.itba.pod.server.repositories.RoomRepository;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;


public class QueryServiceImpl extends QueryMakerGrpc.QueryMakerImplBase {
    private final PatientRepository patientRepository;
    private final FinishedAppointmentRepository finishedAppointmentRepository;
    private final RoomRepository roomRepository;

    public QueryServiceImpl(PatientRepository patientRepository,RoomRepository roomRepository,FinishedAppointmentRepository finishedAppointmentRepository){
        this.patientRepository = patientRepository;
        this.roomRepository=roomRepository;
        this.finishedAppointmentRepository = finishedAppointmentRepository;
    }

    @Override
    public void queryWaitingRooms(Empty request, StreamObserver<Service.PatientsCurrentState> responseObserver){
        List<Patient> waitingRoom = patientRepository.getWaitingRoomList();      // si cambia level del patient es porque sigue en waiting room => no necesito el lock
        Service.PatientsCurrentState.Builder builder = Service.PatientsCurrentState.newBuilder();
        for (Patient patient : waitingRoom){
            builder.addPatients(Service.PatientQueryInfo.newBuilder().setPatient(patient.getPatientName()).setLevelValue(patient.getPatientLevel().ordinal()+1));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void queryCares(Service.Query request, StreamObserver<Service.FinishedAppointmentsState> responseObserver){
        long roomId = request.getRoomIdFilter();
        if ( roomId > roomRepository.getMaxRoomId().get() )
            return;
        List<Appointment> filteredFinishedAppointments = finishedAppointmentRepository.getFinishedAppointmentsList();
        if (request.getRoomIdFilter() != 0){
            filteredFinishedAppointments = filteredFinishedAppointments.stream().filter(appointment -> appointment.getRoomId() == request.getRoomIdFilter()).collect(Collectors.toCollection(ArrayList::new));
        }
        Service.FinishedAppointmentsState.Builder builder = Service.FinishedAppointmentsState.newBuilder();
        for (Appointment appointment : filteredFinishedAppointments){
            builder.addAppointments(Service.FinishedAppointmentQueryInfo.newBuilder().setId(appointment.getRoomId()).setRoomInfo(Service.RoomBasicInfo.newBuilder().setPatient(appointment.getPatient().getPatientName()).setPatientLevelValue(appointment.getPatient().getPatientLevel().ordinal() + 1).setDoctor(appointment.getDoctor().getDoctorName()).setDoctorLevelValue(appointment.getDoctor().getLevel().ordinal() + 1).build()).build());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void queryRooms(Empty request, StreamObserver<Service.RoomsCurrentState> responseObserver) {
        List<Appointment> roomList=roomRepository.getRoomsState();
        roomList.sort(Comparator.comparingLong(Appointment::getRoomId));
        List<Service.RoomFullInfo> roomsInfoList= roomList.stream().map((appointment) -> {
            Service.RoomFullInfo.Builder roomInfoBuilder= Service.RoomFullInfo.newBuilder().setAvailability(true);
            Doctor doctor = appointment.getDoctor();
            if(doctor !=null){
                roomInfoBuilder.setAvailability(false);
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
