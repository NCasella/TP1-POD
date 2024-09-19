package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.exceptions.FailedDoctorPageException;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.models.Notification;

import javax.print.Doc;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class NotificationRepository {
    private final Map<String, BlockingQueue<Notification>> doctorNotificationMap;
    private final DoctorRepository doctorRepository;

    public NotificationRepository(DoctorRepository doctorRepository) {
        this.doctorNotificationMap = new ConcurrentHashMap<>();
        this.doctorRepository = doctorRepository;
    }

    public void notify(String name, Notification notification) {
        addNotification(name, notification);
        //ExecutorService executorService = new
        // future?
        removeNotification(name, notification);
    }

    public synchronized void addNotification(String name, Notification notification){
        BlockingQueue<Notification> list = doctorNotificationMap.computeIfAbsent(name,key -> new LinkedBlockingQueue<>());
        list.add(notification);
    }

    public synchronized void removeNotification(String name, Notification notification){
       BlockingQueue<Notification> notifications = Optional.ofNullable(doctorNotificationMap.get(name)).orElseThrow(FailedDoctorPageException::new);
       // seria de los primeros en la lista, pero como no puedo asegurarme q es el primero, hay q usar remove
       notifications.remove(notification);
    }

    public Notification getNewNotification(String doctorName) {
        return Optional.ofNullable(doctorNotificationMap.get(doctorName).peek()).orElseThrow(FailedDoctorPageException::new);
    }
}
