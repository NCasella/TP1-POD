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
        long roomId = request.getValue();
        if ( roomId > roomsRepository.getMaxRoomId() )    // seguro pues no se pueden eliminar los rooms
            throw new RoomIdNotFoundException(roomId);
        lockService.lock();
        try {
            if (!roomsRepository.isRoomAvailable(roomId))       // lockService me garantiza que no lo pueden ocupar en este bloque
                throw new RoomAlreadyBusyException(roomId);

            appointmentMade = findAttendanceForWaitingPatient(new Appointment(roomId, null, null, null)).orElseThrow(() -> new NoDoctorsAvailableException(roomId));
            final Doctor doctor = appointmentMade.getDoctor();
            notificationRepository.notify(doctor.getDoctorName(), new Notification(doctor.getLevel(), ActionType.STARTED_CARING, appointmentMade.getPatient().getPatientName(), appointmentMade.getPatient().getPatientLevel(), appointmentMade.getRoomId()));
        } finally {
            lockService.unlock();
        }
        responseObserver.onNext(Service.RoomBasicInfo.newBuilder().setPatient(appointmentMade.getPatient().getPatientName()).setPatientLevel(Service.Level.forNumber(appointmentMade.getPatient().getPatientLevel().ordinal()+1)).setDoctor(appointmentMade.getDoctor().getDoctorName()).setDoctorLevel(Service.Level.forNumber(appointmentMade.getDoctor().getLevel().ordinal()+1)).build());
        responseObserver.onCompleted();
    }

    @Override
    public void careAllPatients(Empty request, StreamObserver<Service.AllRoomsFullInfo> responseObserver){
        Service.AllRoomsFullInfo.Builder builder = Service.AllRoomsFullInfo.newBuilder();
        final long maxRoomId = roomsRepository.getMaxRoomId();
        lockService.lock();
        try {
            // dentro del lockService => no se van a ocupar los available rooms
           final List<Patient> waitingRoom = patientRepository.getWaitingRoomListWithPatientsLock();
           final List<Doctor> doctorArray = doctorRepository.getAllDoctorsWithLock();
           final Iterator<Patient> waitingRoomIterator = waitingRoom.iterator();
           for (long roomId = 1; roomId <= maxRoomId; roomId++) {
               if (!roomsRepository.isRoomAvailable(roomId)) {
                   // ya existe un appointment de antes por eso no lo ocupe -> se envia el id
                   builder.addRoomsInfo(Service.RoomFullInfo.newBuilder().setAvailability(false).setRoomInfo(Service.RoomBasicInfo.newBuilder().build()).setId(roomId)).build();
               } else {
                   //si no hay manera de que se ocupe una room se devuelve solo las que se pudieron llenar sin tirar excepcion
                   Optional<Appointment> maybeAppointment = findAttendanceForWaitingPatient(new Appointment(roomId, null, null, null), waitingRoomIterator, doctorArray);
                   if (maybeAppointment.isEmpty()) {
                       // si no se pudo asignar un doctor para los pacientes que quedan doy a entender que quedaron vacias con availability en true
                       builder.addRoomsInfo(Service.RoomFullInfo.newBuilder().setAvailability(true).setId(roomId)).build();
                   } else {
                       // se que va a ser una room llenada ahora por el localdatetime
                       Appointment appointment = maybeAppointment.get();
                       builder.addRoomsInfo(Service.RoomFullInfo.newBuilder().setAvailability(false).setId(roomId).setRoomInfo(Service.RoomBasicInfo.newBuilder().setPatient(appointment.getPatient().getPatientName()).setPatientLevel(Service.Level.forNumber(appointment.getPatient().getPatientLevel().ordinal() + 1)).setDoctor(appointment.getDoctor().getDoctorName()).setDoctorLevel(Service.Level.forNumber(appointment.getDoctor().getLevel().ordinal() + 1)).build()).build());
                       notificationRepository.notify(appointment.getDoctor().getDoctorName(), new Notification(appointment.getDoctor().getLevel(), ActionType.STARTED_CARING, appointment.getPatient().getPatientName(), appointment.getPatient().getPatientLevel(), roomId));
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
       final Doctor doctor = doctorRepository.getDoctor(request.getDoctor());
       doctor.lockDoctor();         // lock necesario para que no haya 2 threads deshaciendo el appointment y registrandolo 2 veces como terminado
       try {                        // y ademas se cambia la availability del doctor -> el lock garantiza que no se está ejecutando otro thread que intente alterar "a la vez" su availability, por ej ejecutando carePatient(), careAllPatients(), setDoctor()
           matchedAppointment = roomsRepository.getAppointment(request.getId(),new Patient(request.getPatient()),doctor);
           roomsRepository.finishAppointment(matchedAppointment);
           doctor.setDisponibility(Availability.AVAILABLE);
           matchedAppointment.setFinishTime(LocalDateTime.now());             // debe estar dentro del bloque de lock porque si al salir de este se llama a algun care(), y desp se hace un discharge y este logra ejecutar hasta despues de esta linea => queda registrado que el doctor atendio primero el ultimo turno, y luego el verdadero primer turno
           notificationRepository.notify(request.getDoctor(), new Notification(matchedAppointment.getDoctor().getLevel(), ActionType.ENDED_CARING, request.getPatient(), matchedAppointment.getPatient().getPatientLevel(), request.getId()));
       } finally {
           doctor.unlockDoctor();
       }
       try {
           finishedAppointmentRepository.addAppointment(matchedAppointment);
       } catch (InterruptedException e) {
           throw new InternalServerException();
       }

       responseObserver.onNext(Service.RoomBasicInfo.newBuilder().setPatient(request.getPatient()).setPatientLevel(Service.Level.forNumber(matchedAppointment.getPatient().getPatientLevel().ordinal()+1)).setDoctor(request.getDoctor()).setDoctorLevel(Service.Level.forNumber(matchedAppointment.getDoctor().getLevel().ordinal()+1)).build());
       responseObserver.onCompleted();
       // devuelvo el objeto para que el cliente le sirva la info de que sucedio
   }


    private static final Comparator<Doctor> ascendingLevelAlphab = (d1, d2) -> {
        if (d1.getLevel().ordinal() == d2.getLevel().ordinal()){
            return d1.getDoctorName().compareTo(d2.getDoctorName());
        }
        return d1.getLevel().ordinal() - d2.getLevel().ordinal();
    };

    private static void unlockDoctorsAndPatients(List<Doctor> doctorArray, List<Patient> waitingRoom) {
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
        if ( patientRepository.isWaitingRoomEmpty() )
            throw new NoPatientsInWaitingRoomException(appointment.getRoomId());

        List<Patient> waitingRoom = patientRepository.getWaitingRoomListWithPatientsLock();     // con lock para que su nivel no pueda ser alterado
        List<Doctor> doctorArray = doctorRepository.getAllDoctorsWithLock();                    // con lock para que su disponibilidad no pueda ser alterada

        Optional<Appointment> result = findAttendanceForWaitingPatient(appointment,waitingRoom.iterator(),doctorArray);
        unlockDoctorsAndPatients(doctorArray, waitingRoom);
        return result;
    }
    private Optional<Appointment> findAttendanceForWaitingPatient(Appointment appointment, Iterator<Patient> waitingRoom, List<Doctor> doctorArray) {
        Patient currentPatient;
        // colección auxiliar para que todos los que puedan atender queden ordenados y solo tenga que sacar el primero
        final PriorityBlockingQueue<Doctor> candidates = new PriorityBlockingQueue<>(10, ascendingLevelAlphab);

        while (waitingRoom.hasNext()) {
            //se obtiene el nivel de risk y veo si hay alguno que matchee
            //necesito chequear que además esten available esos doctors
            currentPatient = waitingRoom.next();
            Level currentPatientsLevel = currentPatient.getPatientLevel();

            for (Doctor doctor : doctorArray) {
                // todos los que cumplan y tengan el nivel más bajo (igual al del paciente)
                // van a la colección auxiliar
                if (doctor.getDisponibility() == Availability.AVAILABLE && doctor.getLevel().ordinal() >= currentPatientsLevel.ordinal()) {
                    candidates.put(doctor);
                }
            }
            if (!candidates.isEmpty()) {
                appointment.setPatient(currentPatient);
                final Doctor doctorMatched = candidates.poll();
                appointment.setDoctorInAppointment(doctorMatched);
                appointment.setStartTime(LocalDateTime.now());

                // cambio el estado del doctor y las salas
                doctorMatched.setDisponibility(Availability.ATTENDING);
                roomsRepository.startAppointment(appointment);
                patientRepository.getWaitingRoom().remove(currentPatient);
                return Optional.of(appointment);
            }
        }
        return Optional.empty();
    }

}
