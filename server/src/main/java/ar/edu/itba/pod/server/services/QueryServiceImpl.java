package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.models.Appointment;
import ar.edu.itba.pod.server.models.Patient;
import ar.edu.itba.pod.server.repositories.FinishedAppointmentRepository;
import ar.edu.itba.pod.server.repositories.PatientRepository;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

public class QueryServiceImpl {
    private final PatientRepository patientRepository;
    private final FinishedAppointmentRepository finishedAppointmentRepository;

    public QueryServiceImpl(PatientRepository patientRepository,FinishedAppointmentRepository finishedAppointmentRepository){
        this.patientRepository = patientRepository;
        this.finishedAppointmentRepository = finishedAppointmentRepository;
    }
    @Override
    public void queryWaitingRooms(Empty request, StreamObserver<Service.PatientsCurrentState> responseObserver){
        BlockingQueue<Patient> waitingRoom = patientRepository.getWaitingRoomCopy();
        Service.PatientsCurrentState.Builder builder = Service.PatientsCurrentState.newBuilder();
        for (Patient patient : waitingRoom){
            builder.addPatients(Service.PatientQueryInfo.newBuilder().setPatient(patient.getPatientName()).setLevelValue(patient.getPatientLevel().ordinal()+1));
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void queryCares(Service.Query request, StreamObserver<Service.FinishedAppointmentsState> responseObserver){
        BlockingQueue<Appointment> finishedAppointments = finishedAppointmentRepository.getFinishedAppointments();
        List<Appointment> filteredFinishedAppointments = finishedAppointments.stream().toList();
        if (request.isInitialized()){
            filteredFinishedAppointments = finishedAppointments.stream().filter(appointment -> appointment.getRoomId() == request.getRoomIdFilter()).collect(Collectors.toCollection(ArrayList::new));
        }
        Service.FinishedAppointmentsState.Builder builder = Service.FinishedAppointmentsState.newBuilder();
        for (Appointment appointment : filteredFinishedAppointments){
            builder.addAppointments(Service.FinishedAppointmentQueryInfo.newBuilder().setId(appointment.getRoomId()).setRoomInfo(Service.RoomBasicInfo.newBuilder().setPatient(appointment.getPatient().getPatientName()).setPatientLevelValue(appointment.getPatient().getPatientLevel().ordinal() + 1).setDoctor(appointment.getDoctor().getDoctorName()).setDoctorLevelValue(appointment.getDoctor().getLevel().ordinal() + 1).build()).build());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}
