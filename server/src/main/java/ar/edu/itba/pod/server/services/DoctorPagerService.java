package ar.edu.itba.pod.server.services;

//package ar.edu.itba.pod.grpc.

import ar.edu.itba.pod.grpc.DoctorPagerGrpc;
import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.exceptions.FailedDoctorPageException;
import ar.edu.itba.pod.server.models.ActionType;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.models.Notification;
import ar.edu.itba.pod.server.repositories.DoctorRepository;
import ar.edu.itba.pod.server.repositories.NotificationRepository;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;

public class DoctorPagerService extends DoctorPagerGrpc.DoctorPagerImplBase {
    private final DoctorRepository doctorRepository;
    private final NotificationRepository notificationRepository;

    public DoctorPagerService(DoctorRepository doctorRepository,
                              NotificationRepository notificationRepository) {
        this.doctorRepository = doctorRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void getNotifications(StringValue request, StreamObserver<Service.Notification> responseObserver) {
        String doctorName = request.getValue();
        Service.Notification notification;
        notificationRepository.registerDoctor(doctorName);
        boolean registered = true;
        while ( registered) {
            // si no hay notificaciones, se bloquea
            try {
                notification = notificationRepository.readNewNotification(doctorName).toGrpc();
            } catch (RuntimeException e) {
                notificationRepository.unregisterDoctor(doctorName);
                throw e;    //! la vuelvo a tirar
            }

            responseObserver.onNext(notification);
            if ( notification.getAction().equals(Service.Action.UNREGISTER) )
                registered = false;
        }
        responseObserver.onCompleted();
        responseObserver.onError(new FailedDoctorPageException());
    }

    @Override
    public void cancelNotifications(StringValue request, StreamObserver<Service.Notification> responseObserver) {
        Doctor doctor = doctorRepository.getDoctor(request.getValue());
        Notification notification = new Notification(doctor.getLevel(), ActionType.UNREGISTER);
        notificationRepository.cancelNotifications(doctor.getDoctorName(), notification);

        responseObserver.onNext(notification.toGrpc());
        responseObserver.onCompleted();

        // genero evento al que esta suscrito el cliente que invoco a getNotifications()
        // y quien llamo tmb va a imprimir lo mismo
    }


}
