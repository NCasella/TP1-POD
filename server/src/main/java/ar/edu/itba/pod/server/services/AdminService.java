package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.AdminServiceGrpc;
import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.models.Disponibility;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.exceptions.DoctorIsAttendingException;
import ar.edu.itba.pod.server.models.Level;
import ar.edu.itba.pod.server.repositories.DoctorRepository;
import ar.edu.itba.pod.server.repositories.RoomRepository;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
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
    public void addDoctor(Service.EnrollmentInfo request, StreamObserver<StringValue> responseObserver) {
        String doctorName=request.getName();
        if(request.getLevel().getNumber()==0||request.getLevel().getNumber()>5)
            throw new IllegalArgumentException("Invalid level parameter");
        Level doctorLevel=Level.valueOf(request.getLevel().toString());
        doctorRepository.addDoctor(doctorName,doctorLevel);
        LOGGER.info("Doctor {} added to register",doctorName);
        responseObserver.onNext(StringValue.of(String.format("Doctor %s (%d) added successfully",doctorName,doctorLevel.getLevelNumber())));
        responseObserver.onCompleted();
    }

    @Override
    public void addRoom(Empty request,  StreamObserver<Int64Value> responseObserver) {
        responseObserver.onNext(Int64Value.of(roomsRepository.addRoom()));
        responseObserver.onCompleted();
    }

    @Override
    public void setDoctor(Service.DoctorAvailabilityInfo request, StreamObserver<Empty> responseObserver) {
        String doctorNameRequest=request.getName();
        if(doctorRepository.getDoctor(doctorNameRequest).getDisponibility()==Disponibility.ATTENDING)
            throw new DoctorIsAttendingException(String.format("Doctor %s is currently attending a patient",doctorNameRequest));
        doctorRepository.setDoctorDisponibility(doctorNameRequest, Disponibility.getDisponibilityFromNumber(request.getAvailability().getNumber()));
    }

    @Override
    public void checkDoctor(StringValue request, StreamObserver<Service.DoctorAvailabilityInfo> responseObserver) {
        Doctor doctor=doctorRepository.getDoctor(request.getValue());
        responseObserver.onNext(Service.DoctorAvailabilityInfo.newBuilder().setName(doctor.getDoctorName()).setAvailabilityValue(doctor.getDisponibility().ordinal()).build());
        responseObserver.onCompleted();
    }
}
