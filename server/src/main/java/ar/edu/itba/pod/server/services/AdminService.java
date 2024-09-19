package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.AdminServiceGrpc;
import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.models.Availability;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.exceptions.DoctorIsAttendingException;
import ar.edu.itba.pod.server.models.Level;
import ar.edu.itba.pod.server.repositories.DoctorRepository;
import ar.edu.itba.pod.server.repositories.RoomRepository;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminService extends AdminServiceGrpc.AdminServiceImplBase {
    private final DoctorRepository doctorRepository;
    private final RoomRepository roomsRepository;
    private final Logger LOGGER= LoggerFactory.getLogger(AdminService.class);

    public AdminService(DoctorRepository doctorRepository, RoomRepository roomsRepository){
        this.doctorRepository=doctorRepository;
        this.roomsRepository=roomsRepository;
    }

    @Override
    public void addDoctor(Service.EnrollmentInfo request, StreamObserver<Service.EnrollmentInfo> responseObserver) {
        String doctorName=request.getName();
        if(request.getLevel().getNumber()<=0||request.getLevel().getNumber()>5)
            throw new IllegalArgumentException("Invalid level parameter");
        Level doctorLevel=Level.valueOf(request.getLevel().toString());
        doctorRepository.addDoctor(doctorName,doctorLevel);
        LOGGER.info("Doctor {} added to register",doctorName);
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }

    @Override
    public void addRoom(Empty request, StreamObserver<UInt64Value> responseObserver) {
        long newRoomId=roomsRepository.addRoom();
        LOGGER.info("created room number {}",newRoomId);
        responseObserver.onNext(UInt64Value.of(newRoomId));
        responseObserver.onCompleted();
    }

    @Override
    public void setDoctor(Service.DoctorAvailabilityRequest request, StreamObserver<Service.CompleteDoctorAvailabilityInfo> responseObserver) {
        String doctorNameRequest=request.getDoctorName();
        if(doctorRepository.getDoctor(doctorNameRequest).getDisponibility()==Availability.ATTENDING)
            throw new DoctorIsAttendingException(String.format("Doctor %s is currently attending a patient",doctorNameRequest));
        LOGGER.info("Consulted doctor {}",doctorNameRequest);
        doctorRepository.setDoctorDisponibility(doctorNameRequest, Availability.getDisponibilityFromNumber(request.getDoctorAvailability().getNumber()));
        checkDoctor(StringValue.of(doctorNameRequest),responseObserver);
    }

    @Override
    public void checkDoctor(StringValue request, StreamObserver<Service.CompleteDoctorAvailabilityInfo> responseObserver) {
        Doctor doctor=doctorRepository.getDoctor(request.getValue());
        responseObserver.onNext(Service.CompleteDoctorAvailabilityInfo.newBuilder()
                .setDoctorName(doctor.getDoctorName())
                .setDoctorLevelValue(doctor.getLevel().getLevelNumber())
                .setDoctorAvailabilityValue(doctor.getDisponibility().getDisponibilityNumber()).build());
        responseObserver.onCompleted();
    }
}
