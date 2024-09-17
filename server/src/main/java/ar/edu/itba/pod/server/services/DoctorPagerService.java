package ar.edu.itba.pod.server.services;

//package ar.edu.itba.pod.grpc.

import ar.edu.itba.pod.grpc.*;
import ar.edu.itba.pod.grpc.Service.*;
import ar.edu.itba.pod.server.exceptions.DoctorNotFoundException;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.repositories.DoctorRepository;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;


public class DoctorPagerService extends DoctorPagerGrpc.DoctorPagerImplBase {
    private final DoctorRepository doctorRepository;

    public DoctorPagerService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Override
    public void getNotifications(StringValue request, StreamObserver<Service.Notification> responseObserver) {
        //deberia suscribirse a los eventos
            // INCLUIDO EL DE CANCEL
        Notification notification = Notification.newBuilder().build();
    }

    @Override
    public void cancelNotifications(StringValue request, StreamObserver<Service.Notification> responseObserver) {
        Doctor doctor = doctorRepository.getDoctor(request.getValue()).orElseThrow(DoctorNotFoundException::new);
        // genero evento al q esta suscrito el cliente q invoco a getNotif()
        // y quien llamo tmb va a imprimir lo mismo
    }
}
