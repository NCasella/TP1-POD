package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.AdminServiceGrpc;
import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.models.Disponibility;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.models.Level;
import ar.edu.itba.pod.server.repositories.DoctorRepository;
import ar.edu.itba.pod.server.repositories.RoomsRepository;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminService extends AdminServiceGrpc.AdminServiceImplBase {
    private final DoctorRepository doctorRepository;
    private final RoomsRepository roomsRepository;
    private final Logger LOGGER= LoggerFactory.getLogger(AdminService.class);

    public AdminService(DoctorRepository doctorRepository, RoomsRepository roomsRepository){
        this.doctorRepository=doctorRepository;
        this.roomsRepository=roomsRepository;
    }

    @Override
    public void addDoctor(Service.EnrollmentInfo request, StreamObserver<StringValue> responseObserver) {
        String doctorName=request.getName();
        if(request.getLevel().getNumber()==0)
            throw new IllegalArgumentException("Invalid level parameter");
        Level doctorLevel=Level.valueOf(request.getLevel().toString());
        doctorRepository.addDoctor(doctorName,doctorLevel);
        LOGGER.info("Doctor {} added to register",doctorName);
        responseObserver.onNext(StringValue.of(String.format("Doctor %s (%d) added successfully",doctorName,doctorLevel.getLevelNumber())));
        responseObserver.onCompleted();
    }

    @Override
    public void addRoom(Empty request, StreamObserver<Service.Id> responseObserver) {
        responseObserver.onNext(Service.Id.newBuilder().setId(roomsRepository.addRoom()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void setDoctor(Service.DoctorAvailabilityInfo request, StreamObserver<Empty> responseObserver) {
        doctorRepository.setDoctorDisponibility(request.getName(), Disponibility.getDisponibilityFromNumber(request.getAvailability().getNumber()));
    }

    @Override
    public void checkDoctor(StringValue request, StreamObserver<Service.DoctorAvailabilityInfo> responseObserver) {
        Doctor doctor=doctorRepository.getDoctor(request.getValue());
        responseObserver.onNext(Service.DoctorAvailabilityInfo.newBuilder().setName(doctor.getDoctorName()).setAvailabilityValue(doctor.getDisponibility().ordinal()).build());
        responseObserver.onCompleted();
    }
}
