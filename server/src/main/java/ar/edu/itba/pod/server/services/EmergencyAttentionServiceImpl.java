package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.EmergencyAttentionGrpc;
import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.repositories.DoctorRepository;
import ar.edu.itba.pod.server.repositories.NotificationRepository;
import ar.edu.itba.pod.server.repositories.PatientRepository;
import ar.edu.itba.pod.server.repositories.RoomRepository;
import com.google.protobuf.Empty;
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
    private Lock lockService = new ReentrantLock(true);

    public EmergencyAttentionServiceImpl(PatientRepository patientRepository, DoctorRepository doctorRepository, RoomRepository roomsRepository,
                                         NotificationRepository notificationRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.roomsRepository = roomsRepository;
        this.notificationRepository = notificationRepository;
    }
    //con un appointment que solo tenga el numero de sala lo completo con la info que falta (doctor y paciente)
    //chequear tipos con los definidos en api
    @Override
    public void carePatient(com.google.protobuf.Int64Value request, StreamObserver<Service.RoomBasicInfo> responseObserver){
        //chequeo si existe la room que me están mandando en principio
        lockService.lock();
        List<Long> availableRooms = roomsRepository.getAvailableRooms();
        long roomId = request.getValue();
        boolean isAvailable = availableRooms.stream().anyMatch(Predicate.isEqual(roomId));

        if (!isAvailable){
           List<Appointment> unavailableRooms = roomsRepository.getUnavailableRooms();
           Predicate<Appointment> condition = app -> app.getRoomId() == (roomId);
           boolean exists = unavailableRooms.stream().anyMatch(condition);

           if (exists){
               throw new RoomAlreadyBusyException(roomId);
           }else {
               throw new RoomIdNotFoundException(roomId);
           }
        }

        //llamado a metodo private que aplica el algoritmo de selección de paciente y doctor para la atención
        //obtengo el par definitivo a partir del que tenía
        //o bien continuo lanzando la excepcion hasta el Exception Handler
        // si ninguno coincide entonces se tiene que devolver excepcion
        //del lado del server manejo appointment y desde ahí reviso el estado interno del sistema
        Appointment appointmentMade = findAttendanceForWaitingPatient(new Appointment(roomId, null, null, null)).orElseThrow(() -> new NoDoctorsAvailableException(roomId));
        Doctor doctor = appointmentMade.getDoctor();
        notificationRepository.notify(doctor.getDoctorName(),new Notification(doctor.getLevel(),ActionType.STARTED_CARING,appointmentMade.getPatient().getPatientName(),appointmentMade.getPatient().getPatientLevel(),appointmentMade.getRoomId()));
        lockService.unlock();
        responseObserver.onNext(Service.RoomBasicInfo.newBuilder().setPatient(appointmentMade.getPatient().getPatientName()).setPatientLevel(Service.Level.forNumber(appointmentMade.getPatient().getPatientLevel().ordinal()+1)).setDoctor(appointmentMade.getDoctor().getDoctorName()).setDoctorLevel(Service.Level.forNumber(appointmentMade.getDoctor().getLevel().ordinal()+1)).build());
        responseObserver.onCompleted();
    }

    //chequear que es lo que se devuelve finalmente
    //acá incluso queda más comodo tener la lista completa de rooms sin separar por available
    @Override
    public void careAllPatients(Empty request, StreamObserver<Service.AllRoomsFullInfo> responseObserver){
       List<Long> availableRoomIds = roomsRepository.getAvailableRooms();
       //el orden se podría asegurar con una priority queue como en el caso de los patients
       availableRoomIds.sort(Long::compareTo);

       Service.AllRoomsFullInfo.Builder builder = Service.AllRoomsFullInfo.newBuilder();
       for (long roomId = 1; roomId < roomsRepository.getMaxRoomId().get(); roomId++){
           long id = roomId;
           if (roomsRepository.getUnavailableRooms().stream().anyMatch((appointment) -> appointment.getRoomId() == id)){
               //doy a entender que ya existe un appointment de antes por eso no lo ocupe
               //solo mando el id
               builder.addRoomsInfo(Service.RoomFullInfo.newBuilder().setAvailability(false).setRoomInfo(Service.RoomBasicInfo.newBuilder().build()).setId(id)).build();
           }else {
               //si no hay manera de que se ocupe una room se devuelve solo las que se pudieron llenar sin tirar excepcion
               Optional<Appointment> maybeAppointment =  findAttendanceForWaitingPatient(new Appointment(roomId, null, null, null));
               if (maybeAppointment.isEmpty()){
                    //si no se pudo asignar un doctor para los pacientes que quedan doy a entender que quedaron vacias con availability en true
                    builder.addRoomsInfo(Service.RoomFullInfo.newBuilder().setAvailability(true).setId(id)).build();
               }else{
                   //se que va a ser una room llenada ahora por el localdatetime
                   Appointment appointment = maybeAppointment.get();
                   builder.addRoomsInfo(Service.RoomFullInfo.newBuilder().setAvailability(false).setId(id).setRoomInfo(Service.RoomBasicInfo.newBuilder().setPatient(appointment.getPatient().getPatientName()).setPatientLevel(Service.Level.forNumber(appointment.getPatient().getPatientLevel().ordinal()+1)).setDoctor(appointment.getDoctor().getDoctorName()).setDoctorLevel(Service.Level.forNumber(appointment.getDoctor().getLevel().ordinal()+1)).build()).build());
                   notificationRepository.notify(appointment.getDoctor().getDoctorName(),new Notification(appointment.getDoctor().getLevel(),ActionType.STARTED_CARING,appointment.getPatient().getPatientName(),appointment.getPatient().getPatientLevel(),id));
               }
           }
       }
       responseObserver.onNext(builder.build());
       responseObserver.onCompleted();
    }
   @Override
   public void dischargePatient(Service.RoomDischargeInfo request, StreamObserver<Service.RoomBasicInfo> responseObserver) {
       lockService.lock();
       if (doctorRepository.getAllDoctors().stream().noneMatch(doctor -> doctor.equals(new Doctor(request.getDoctor(), null)))) {
           throw new DoctorNotFoundException(request.getDoctor());
       }
       Appointment appointment = new Appointment(request.getId(), new Patient(request.getPatient(), null, null), new Doctor(request.getDoctor(), null), null);
       if (roomsRepository.getUnavailableRooms().stream().noneMatch(app -> app.equals(appointment))) {
           throw new AppointmentNotFoundException();
       }
       //debería poder eliminar el elemento por reconociendo el objeto por el equals
       int index = roomsRepository.getUnavailableRooms().indexOf(appointment);
       Appointment matchedAppointment = roomsRepository.getUnavailableRooms().get(index);
       roomsRepository.getUnavailableRooms().remove(matchedAppointment);
       appointment.getDoctor().setDisponibility(Availability.AVAILABLE);
       roomsRepository.getAvailableRooms().add(appointment.getRoomId());
       notificationRepository.notify(request.getDoctor(),new Notification(matchedAppointment.getDoctor().getLevel(),ActionType.ENDED_CARING, request.getPatient(),matchedAppointment.getPatient().getPatientLevel(), request.getId()));
       lockService.unlock();
       responseObserver.onNext(Service.RoomBasicInfo.newBuilder().setPatient(request.getPatient()).setPatientLevel(Service.Level.forNumber(matchedAppointment.getPatient().getPatientLevel().ordinal()+1)).setDoctor(request.getDoctor()).setDoctorLevel(Service.Level.forNumber(matchedAppointment.getDoctor().getLevel().ordinal()+1)).build());
       responseObserver.onCompleted();
       //devuelvo el objeto para que el cliente le sirva la info de que sucedio
   }
    private Optional<Appointment> findAttendanceForWaitingPatient(Appointment appointment) {
        //obtengo la lista de pacientes y la recorro en busqueda de un match
        //get waiting room manejando una copia de la coleccion
        //poner var de todos los patients en busy -> metodo (locks en cada uno)
        BlockingQueue<Patient> waitingRoom = patientRepository.getWaitingRoomCopy();
        for (Patient patient : waitingRoom){
            patient.lockPatient();
        }
        Iterator<Patient> patientIterator = waitingRoom.iterator();

        Comparator<Doctor> ascendingLevelAlphab = (d1, d2) -> {
            if (d1.getLevel().ordinal() == d2.getLevel().ordinal()){
                return d1.getDoctorName().compareTo(d2.getDoctorName());
            }
            return d1.getLevel().ordinal() - d2.getLevel().ordinal();
        };

        ArrayList<Doctor> doctorArray = doctorRepository.getAllDoctors();
        //armo colección auxiliar para que todos los que puedan atender queden ordenados y solo tenga que sacar el primero
        Queue<Doctor> candidates = new PriorityBlockingQueue<>(10, ascendingLevelAlphab);
        for (Doctor doctor : doctorArray){
            doctor.lockDoctor();
        }

        while (patientIterator.hasNext()){
            //se obtiene el nivel de risk y veo si hay alguno que matchee
            //necesito chequear que además esten available esos doctors
            Patient currentPatient = patientIterator.next(); //down(
            Level currentPatientsLevel = currentPatient.getPatientLevel();

            for(Doctor doctor : doctorArray){
                //todos los que cumplan y tengan el nivel más bajo (igual al del paciente)
                // van a la colección auxiliar
                if (doctor.getDisponibility() == Availability.AVAILABLE && doctor.getLevel().ordinal() >= currentPatientsLevel.ordinal()){
                    candidates.add(doctor);
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
                for (Patient patient : waitingRoom){
                    patient.unlockPatient();
                }
                for (Doctor doctor : doctorArray){
                    doctor.unlockDoctor();
                }
                //dejo el lock de patient si encontró doctor
                return Optional.of(appointment);
            }
            //deja el lock de patient si no encontro ningun doctor
        }
        for (Patient patient : waitingRoom){
            patient.unlockPatient();
        }
        for (Doctor doctor : doctorArray){
            doctor.unlockDoctor();
        }
        return Optional.empty();
    }


}
