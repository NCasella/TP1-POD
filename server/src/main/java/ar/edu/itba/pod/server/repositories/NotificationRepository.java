package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.ActionType;
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

    public void registerDoctor(String name) {
        // lo hago antes para q se bloquee x menos tiempo
        Doctor doctor = doctorRepository.getDoctor(name);
        final BlockingQueue<Notification> notificationQueue = new LinkedBlockingQueue<>();
        notificationQueue.add(new Notification(doctor.getLevel(), ActionType.REGISTER));
        //
        // x ahora: (pero necesito q sea atomico)
        if (doctorNotificationMap.containsKey(name)) {
            throw new DoctorAlreadyRegisteredException(name);
        }
        doctorNotificationMap.put(name, notificationQueue);
        // todo: Optional.ofNullable(doctorNotificationMap.putIfAbsent(name,notificationQueue)).orElseThrow(()->new DoctorAlreadyRegisteredForPagerException(name));
    }

    public void notify(String name, Notification notification) {
        if (!doctorNotificationMap.containsKey(name))
            return;
        addNotification(name, notification);
    }

    private synchronized void addNotification(String name, Notification notification){
        BlockingQueue<Notification> list = Optional.ofNullable(doctorNotificationMap.get(name)).orElseThrow(()-> new DoctorNotFoundException(name));
        list.add(notification);
    }

    // prefiero leer y eliminar desde aca asi no queda visible desde afuera la queue
    public Notification readNewNotification(String name) {
        //! lee y elimina la notificacion
        BlockingQueue<Notification> queue = Optional.ofNullable(doctorNotificationMap.get(name)).orElseThrow(() -> new DoctorNotificationsNotFoundException(name));
        Notification notification;
        try {
            //notification = Optional.of(queue.take()).orElseThrow(()-> new DoctorFirstNotificationNotFoundException(name));
            notification = queue.take();
        } catch (InterruptedException e) {
            throw new DoctorFirstNotificationNotFoundException(name);
        }

        //! quito doctor del mapa si nadie está suscrito
        if (notification.isUnregistered() && queue.isEmpty())
         //   synchronized (this) { doctorNotificationMap.remove(name); }
            doctorNotificationMap.remove(name);
        return notification;
    }
}
