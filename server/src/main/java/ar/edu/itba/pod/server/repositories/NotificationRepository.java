package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.exceptions.*;
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
        if ( doctorNotificationMap.containsKey(name)) {
            throw new DoctorAlreadyRegisteredForPagerException(name);
        }
        doctorNotificationMap.put(name, new LinkedBlockingQueue<>());
    }

    public void notify(String name, Notification notification) {
        if (!doctorNotificationMap.containsKey(name))
            return;
        addNotification(name, notification);
    }

    private void addNotification(String name, Notification notification){
        BlockingQueue<Notification> list = Optional.ofNullable(doctorNotificationMap.get(name)).orElseThrow(()-> new DoctorNotFoundException(name));
        list.add(notification);
    }

    // prefiero leer y eliminar desde aca asi no queda visible desde afuera la queue
    public Notification readNewNotification(String name) {
        //! lee y elimina la notificacion
        BlockingQueue<Notification> queue = Optional.ofNullable(doctorNotificationMap.get(name)).orElseThrow(() -> new DoctorNotificationsNotFoundException(name));
        Notification notification = Optional.ofNullable(queue.poll()).orElseThrow(()-> new DoctorFirstNotificationNotFoundException(name));

        //! quito doctor del mapa si nadie está suscrito
        if (notification.isUnregistered() && queue.isEmpty())
            doctorNotificationMap.remove(name);
        return notification;
    }
}
