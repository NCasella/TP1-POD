package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.AdminServiceGrpc;
import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.exceptions.DoctorIsAttendingException;
import ar.edu.itba.pod.server.repositories.DoctorRepository;
import ar.edu.itba.pod.server.repositories.NotificationRepository;
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
    private final NotificationRepository notificationRepository;

    public AdminService(DoctorRepository doctorRepository, RoomRepository roomsRepository, NotificationRepository notificationRepository) {
        this.doctorRepository=doctorRepository;
        this.roomsRepository=roomsRepository;
        this.notificationRepository=notificationRepository;
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
        String doctorNameRequest = request.getDoctorName();
        Doctor doctor=doctorRepository.getDoctor(doctorNameRequest);
        if(doctor.getDisponibility()==Availability.ATTENDING)
            throw new DoctorIsAttendingException(String.format("Doctor %s is currently attending a patient",doctorNameRequest));
        LOGGER.info("Consulted doctor {}",doctorNameRequest);
        Availability availability = Availability.getDisponibilityFromNumber(request.getDoctorAvailability().getNumber());
        doctorRepository.setDoctorDisponibility(doctorNameRequest,availability);

        Notification notification = new Notification(doctor.getLevel(), ActionType.ofAvailabilty(availability));
        notificationRepository.notify(doctorNameRequest,notification);
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
