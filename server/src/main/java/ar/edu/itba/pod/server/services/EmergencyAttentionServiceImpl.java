package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.EmergencyAttentionGrpc;
import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.repositories.*;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.grpc.stub.StreamObserver;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public class EmergencyAttentionServiceImpl extends EmergencyAttentionGrpc.EmergencyAttentionImplBase{
    private final RoomRepository roomsRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationRepository notificationRepository;
    private final FinishedAppointmentRepository finishedAppointmentRepository;
    private final Lock lockService = new ReentrantLock(true);

    public EmergencyAttentionServiceImpl(PatientRepository patientRepository, DoctorRepository doctorRepository, RoomRepository roomsRepository,
                                         NotificationRepository notificationRepository, FinishedAppointmentRepository finishedAppointmentRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.roomsRepository = roomsRepository;
        this.finishedAppointmentRepository = finishedAppointmentRepository;
        this.notificationRepository = notificationRepository;
    }

    //con un appointment que solo tenga el numero de sala lo completo con la info que falta (doctor y paciente)
    //chequear tipos con los definidos en api
    @Override
    public void carePatient(Int64Value request, StreamObserver<Service.RoomBasicInfo> responseObserver){
        //chequeo si existe la room que me están mandando en principio
        Appointment appointmentMade;
        lockService.lock();
        try {
            long roomId = request.getValue();
            if (!roomsRepository.isRoomAvailable(roomId)) {
                List<Appointment> unavailableRooms = roomsRepository.getUnavailableRooms();
                final Predicate<Appointment> condition = app -> app.getRoomId() == (roomId);
                boolean exists = unavailableRooms.stream().anyMatch(condition);

                if (exists) {
                    throw new RoomAlreadyBusyException(roomId);
                } else {
                    throw new RoomIdNotFoundException(roomId);
                }
            }

            //llamado a metodo private que aplica el algoritmo de selección de paciente y doctor para la atención
            //obtengo el par definitivo a partir del que tenía
            //o bien continuo lanzando la excepcion hasta el Exception Handler
            // si ninguno coincide entonces se tiene que devolver excepcion
            //del lado del server manejo appointment y desde ahí reviso el estado interno del sistema
            appointmentMade = findAttendanceForWaitingPatient(new Appointment(roomId, null, null, null)).orElseThrow(() -> new NoDoctorsAvailableException(roomId));
            final Doctor doctor = appointmentMade.getDoctor();
            notificationRepository.notify(doctor.getDoctorName(), new Notification(doctor.getLevel(), ActionType.STARTED_CARING, appointmentMade.getPatient().getPatientName(), appointmentMade.getPatient().getPatientLevel(), appointmentMade.getRoomId()));
        } finally {
            lockService.unlock();
        }
        responseObserver.onNext(Service.RoomBasicInfo.newBuilder().setPatient(appointmentMade.getPatient().getPatientName()).setPatientLevel(Service.Level.forNumber(appointmentMade.getPatient().getPatientLevel().ordinal()+1)).setDoctor(appointmentMade.getDoctor().getDoctorName()).setDoctorLevel(Service.Level.forNumber(appointmentMade.getDoctor().getLevel().ordinal()+1)).build());
        responseObserver.onCompleted();
    }

    //chequear que es lo que se devuelve finalmente
    //acá incluso queda más comodo tener la lista completa de rooms sin separar por available
    @Override
    public void careAllPatients(Empty request, StreamObserver<Service.AllRoomsFullInfo> responseObserver){
        Service.AllRoomsFullInfo.Builder builder = Service.AllRoomsFullInfo.newBuilder();
        final long maxRoomId = roomsRepository.getMaxRoomId().get();
        lockService.lock();
        try {
           List<Long> availableRoomIds = roomsRepository.getAvailableRooms();
           //el orden se podría asegurar con una priority queue como en el caso de los patients
           availableRoomIds.sort(Long::compareTo);

           ArrayList<Patient> waitingRoom = patientRepository.getWaitingRoomListWithPatientsLock();
           ArrayList<Doctor> doctorArray = doctorRepository.getAllDoctorsWithLock();
           Iterator<Patient> waitingRoomIterator = waitingRoom.iterator();
           for (long roomId = 1; roomId < maxRoomId; roomId++) {
               long id = roomId;
               if (!availableRoomIds.contains(id)) {
                   //doy a entender que ya existe un appointment de antes por eso no lo ocupe
                   //solo mando el id
                   builder.addRoomsInfo(Service.RoomFullInfo.newBuilder().setAvailability(false).setRoomInfo(Service.RoomBasicInfo.newBuilder().build()).setId(id)).build();
               } else {
                   //si no hay manera de que se ocupe una room se devuelve solo las que se pudieron llenar sin tirar excepcion
                   Optional<Appointment> maybeAppointment = findAttendanceForWaitingPatient(new Appointment(roomId, null, null, null), waitingRoomIterator, doctorArray);
                   if (maybeAppointment.isEmpty()) {
                       //si no se pudo asignar un doctor para los pacientes que quedan doy a entender que quedaron vacias con availability en true
                       builder.addRoomsInfo(Service.RoomFullInfo.newBuilder().setAvailability(true).setId(id)).build();
                   } else {
                       //se que va a ser una room llenada ahora por el localdatetime
                       Appointment appointment = maybeAppointment.get();
                       builder.addRoomsInfo(Service.RoomFullInfo.newBuilder().setAvailability(false).setId(id).setRoomInfo(Service.RoomBasicInfo.newBuilder().setPatient(appointment.getPatient().getPatientName()).setPatientLevel(Service.Level.forNumber(appointment.getPatient().getPatientLevel().ordinal() + 1)).setDoctor(appointment.getDoctor().getDoctorName()).setDoctorLevel(Service.Level.forNumber(appointment.getDoctor().getLevel().ordinal() + 1)).build()).build());
                       notificationRepository.notify(appointment.getDoctor().getDoctorName(), new Notification(appointment.getDoctor().getLevel(), ActionType.STARTED_CARING, appointment.getPatient().getPatientName(), appointment.getPatient().getPatientLevel(), id));
                   }
               }
           }
           unlockDoctorsAndPatients(doctorArray, waitingRoom);
       } finally {
               lockService.unlock();
       }
       responseObserver.onNext(builder.build());
       responseObserver.onCompleted();
    }


   @Override
   public void dischargePatient(Service.RoomDischargeInfo request, StreamObserver<Service.RoomBasicInfo> responseObserver) {
       Appointment matchedAppointment;
       lockService.lock();
       try {
           final Doctor doctor = doctorRepository.getDoctor(request.getDoctor());
           matchedAppointment = roomsRepository.getAppointment(request.getId(),new Patient(request.getPatient()),new Doctor(request.getDoctor()));

           doctor.lockDoctor();                                                 // necesito el lock pues si justo se corta entre finishApp() y setear el estado del doctor en Available,
           roomsRepository.finishAppointment(matchedAppointment);               //      si se solicita con ver los estados de los rooms y se consulta por el estado del doctor,
           doctor.setDisponibility(Availability.AVAILABLE);                     //      se obtiene que el room esta libre pero que el doctor sigue atendiendo => es inconsistente
           doctor.unlockDoctor();
           matchedAppointment.setFinishTime(LocalDateTime.now());
           try {
               finishedAppointmentRepository.addAppointment(matchedAppointment);
           } catch (InterruptedException e) {
               throw new InternalServerException();
           }
           notificationRepository.notify(request.getDoctor(), new Notification(matchedAppointment.getDoctor().getLevel(), ActionType.ENDED_CARING, request.getPatient(), matchedAppointment.getPatient().getPatientLevel(), request.getId()));
       } finally {
           lockService.unlock();
       }
       responseObserver.onNext(Service.RoomBasicInfo.newBuilder().setPatient(request.getPatient()).setPatientLevel(Service.Level.forNumber(matchedAppointment.getPatient().getPatientLevel().ordinal()+1)).setDoctor(request.getDoctor()).setDoctorLevel(Service.Level.forNumber(matchedAppointment.getDoctor().getLevel().ordinal()+1)).build());
       responseObserver.onCompleted();
       //devuelvo el objeto para que el cliente le sirva la info de que sucedio
   }


    private static final Comparator<Doctor> ascendingLevelAlphab = (d1, d2) -> {
        if (d1.getLevel().ordinal() == d2.getLevel().ordinal()){
            return d1.getDoctorName().compareTo(d2.getDoctorName());
        }
        return d1.getLevel().ordinal() - d2.getLevel().ordinal();
    };

    private static void unlockDoctorsAndPatients(ArrayList<Doctor> doctorArray, ArrayList<Patient> waitingRoom) {
        for (Doctor doctor : doctorArray){
            doctor.unlockDoctor();
        }
        for (Patient patient : waitingRoom) {
            patient.unlockPatient();
        }
    }

    private Optional<Appointment> findAttendanceForWaitingPatient(Appointment appointment) {
        //obtengo la lista de pacientes y la recorro en busqueda de un match
        //get waiting room manejando una copia de la coleccion

        ArrayList<Patient> waitingRoom = patientRepository.getWaitingRoomListWithPatientsLock();
        ArrayList<Doctor> doctorArray = doctorRepository.getAllDoctorsWithLock();

        Optional<Appointment> result = findAttendanceForWaitingPatient(appointment,waitingRoom.iterator(),doctorArray);
        unlockDoctorsAndPatients(doctorArray, waitingRoom);
        return result;
    }
    private Optional<Appointment> findAttendanceForWaitingPatient(Appointment appointment, Iterator<Patient> waitingRoom, ArrayList<Doctor> doctorArray) {
        Patient currentPatient;
        //armo colección auxiliar para que todos los que puedan atender queden ordenados y solo tenga que sacar el primero
        final PriorityBlockingQueue<Doctor> candidates = new PriorityBlockingQueue<>(10, ascendingLevelAlphab);

        while (waitingRoom.hasNext()) {
            //se obtiene el nivel de risk y veo si hay alguno que matchee
            //necesito chequear que además esten available esos doctors
            currentPatient = waitingRoom.next();
            Level currentPatientsLevel = currentPatient.getPatientLevel();

            for (Doctor doctor : doctorArray) {
                //todos los que cumplan y tengan el nivel más bajo (igual al del paciente)
                // van a la colección auxiliar
                if (doctor.getDisponibility() == Availability.AVAILABLE && doctor.getLevel().ordinal() >= currentPatientsLevel.ordinal()) {
                    candidates.put(doctor);
                }
            }
            if (!candidates.isEmpty()) {
                //reutilizo el objeto que recibí -> check
                appointment.setPatient(currentPatient);
                final Doctor doctorMatched = candidates.poll();
                appointment.setDoctorInAppointment(new Doctor(doctorMatched.getDoctorName(), doctorMatched.getLevel()));
                appointment.setStartTime(LocalDateTime.now());
                //cambio el estado del doctor y las salas

                doctorMatched.setDisponibility(Availability.ATTENDING);
                roomsRepository.startAppointment(appointment);
                patientRepository.getWaitingRoom().remove(currentPatient);
                return Optional.of(appointment);
            }
        }
        return Optional.empty();
    }


/*
    private Optional<Appointment> findAttendanceForWaitingPatient(Appointment appointment) {
        //obtengo la lista de pacientes y la recorro en busqueda de un match
        //get waiting room manejando una copia de la coleccion
        //poner var de todos los patients en busy -> metodo (locks en cada uno)
        ArrayList<Patient> waitingRoom = patientRepository.getWaitingRoomList();        //! lista respeta orden de queue
        for (Patient patient : waitingRoom){
            patient.lockPatient();
        }
        Iterator<Patient> patientIterator = waitingRoom.iterator();

        ArrayList<Doctor> doctorArray = doctorRepository.getAllDoctors();
        //armo colección auxiliar para que todos los que puedan atender queden ordenados y solo tenga que sacar el primero
        PriorityBlockingQueue<Doctor> candidates = new PriorityBlockingQueue<>(10, ascendingLevelAlphab);
        for (Doctor doctor : doctorArray){
            doctor.lockDoctor();
        }
        Optional<Appointment> result = Optional.empty();
        Patient currentPatient;
        while (patientIterator.hasNext()){
            //se obtiene el nivel de risk y veo si hay alguno que matchee
            //necesito chequear que además esten available esos doctors
            currentPatient = patientIterator.next(); //down(
            Level currentPatientsLevel = currentPatient.getPatientLevel();

            for(Doctor doctor : doctorArray){
                //todos los que cumplan y tengan el nivel más bajo (igual al del paciente)
                // van a la colección auxiliar
                if (doctor.getDisponibility() == Availability.AVAILABLE && doctor.getLevel().ordinal() >= currentPatientsLevel.ordinal()){
                    candidates.put(doctor);
                }
            }
            if (!candidates.isEmpty()){
                //reutilizo el objeto que recibí -> check
                appointment.setPatient(currentPatient);
                Doctor doctorMatched = candidates.poll();
                appointment.setDoctorInAppointment(doctorMatched);
                appointment.setStartTime(LocalDateTime.now());
                //cambio el estado del doctor y las salas
                doctorMatched.setDisponibility(Availability.ATTENDING);
                roomsRepository.getAvailableRooms().remove(appointment.getRoomId());
                roomsRepository.getUnavailableRooms().add(appointment);
                patientRepository.getWaitingRoom().remove(currentPatient);

                currentPatient.unlockPatient();
                //dejo el lock de patient si encontró doctor
                result = Optional.of(appointment);
                break;
            }
            //deja el lock de patient si no encontro ningun doctor
            currentPatient.unlockPatient();
        }
        while (patientIterator.hasNext()){
            currentPatient= patientIterator.next();
            currentPatient.unlockPatient();
        }
        for (Doctor doctor : doctorArray){
            doctor.unlockDoctor();
        }
        return result;
    }
*/

}
