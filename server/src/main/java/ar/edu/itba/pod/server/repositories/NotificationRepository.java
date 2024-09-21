package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.ActionType;
import ar.edu.itba.pod.server.models.Doctor;
import ar.edu.itba.pod.server.models.Notification;

import java.util.*;
import java.util.concurrent.*;

public class NotificationRepository {
    private final Map<String, BlockingQueue<Notification>> doctorNotificationMap;
    private final DoctorRepository doctorRepository;

    public NotificationRepository(DoctorRepository doctorRepository) {
        this.doctorNotificationMap = new ConcurrentHashMap<>();
        this.doctorRepository = doctorRepository;
    }

    public void registerDoctor(String name) {
        // lo hago antes para q se bloquee x menos tiempo
        Doctor doctor = doctorRepository.getDoctor(name);
        final BlockingQueue<Notification> notificationQueue = new LinkedBlockingQueue<>();
        notificationQueue.add(new Notification(doctor.getLevel(), ActionType.REGISTER));
        //

        if ( null != doctorNotificationMap.putIfAbsent(name, notificationQueue))
            throw new DoctorAlreadyRegisteredForPagerException(name);
    }

    public void notify(String name, Notification notification) {
        if (doctorNotificationMap.containsKey(name))
            addNotification(name, notification);
    }

    private synchronized void addNotification(String name, Notification notification){
        BlockingQueue<Notification> list = Optional.ofNullable(doctorNotificationMap.get(name)).orElseThrow(()-> new DoctorNotRegisteredForPagerException(name));
        list.add(notification);
    }

    // prefiero leer y eliminar desde aca asi no queda visible desde afuera la queue
    public Notification readNewNotification(String name) {
        //! lee y elimina la notificacion
        BlockingQueue<Notification> queue = Optional.ofNullable(doctorNotificationMap.get(name)).orElseThrow(() -> new DoctorNotificationsNotFoundException(name));
        Notification notification;
        try {
            notification = queue.take();
        } catch (InterruptedException e) {
            throw new DoctorFirstNotificationNotFoundException(name);
        }

        //! quito doctor del mapa si ya no quedan mas notificacion por leer
        if (notification.isUnregistered() && queue.isEmpty())
            unregisterDoctor(name);
        return notification;
    }

    public synchronized void unregisterDoctor(String name) {
        doctorNotificationMap.remove(name);
        System.out.println(doctorNotificationMap.size());
    }

    public void cancelNotifications(String name, Notification notification) {
        if (!doctorNotificationMap.containsKey(name)) {
            throw new DoctorNotRegisteredForPagerException(name);
        }
        addNotification(name, notification);
    }
}
