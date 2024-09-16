package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.EmergencyAttentionGrpc;
import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.*;
import ar.edu.itba.pod.server.repositories.DoctorRepository;
import ar.edu.itba.pod.server.repositories.PatientRepository;
import ar.edu.itba.pod.server.repositories.RoomsRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;

public class EmergencyAttentionServiceImpl extends EmergencyAttentionGrpc.EmergencyAttentionImplBase{
    private final RoomsRepository roomsRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public EmergencyAttentionServiceImpl(PatientRepository patientRepository, DoctorRepository doctorRepository, RoomsRepository roomsRepository){
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.roomsRepository = roomsRepository;
    }
    //con un appointment que solo tenga el numero de sala lo completo con la info que falta (doctor y paciente)
    //chequear tipos con los definidos en api
    public Appointment carePatient(Appointment appointment){
        //chequeo si existe la room que me están mandando en principio
        List<Long> availableRooms = roomsRepository.getAvailableRooms();
        boolean isAvailable = availableRooms.stream().anyMatch(Predicate.isEqual(appointment.getRoomId()));

        if (!isAvailable){
           List<Appointment> unavailableRooms = roomsRepository.getUnavailableRooms();
           Predicate<Appointment> condition = app -> app.getRoomId() == appointment.getRoomId();
           boolean exists = unavailableRooms.stream().anyMatch(condition);

           if (exists){
               throw new RoomAlreadyBusyException(appointment.getRoomId());
           }else {
               throw new RoomIdNotFoundException(appointment.getRoomId());
           }
        }

        //llamado a metodo private que aplica el algoritmo de selección de paciente y doctor para la atención
        //obtengo el par definitivo a partir del que tenía
        //o bien continuo lanzando la excepcion hasta el Exception Handler
        // si ninguno coincide entonces se tiene que devolver excepcion
        return findAttendanceForWaitingPatient(appointment).orElseThrow(() -> new NoDoctorsAvailableException(appointment.getRoomId()));
    }

    //chequear que es lo que se devuelve finalmente
    //acá incluso queda más comodo tener la lista completa de rooms sin separar por available
    public List<Appointment> careAllPatients(){
       List<Long> availableRoomIds = roomsRepository.getAvailableRooms();
       //el orden se podría asegurar con una priority queue como en el caso de los patients
       availableRoomIds.sort(Long::compareTo);
       List<Long> availableRooms = roomsRepository.getAvailableRooms();

       List<Appointment> allAppointments = new ArrayList<>();
       for (long roomId = 1; roomId <= roomsRepository.getMaxRoomId(); roomId++){
           long id = roomId;
           if (roomsRepository.getUnavailableRooms().stream().anyMatch((appointment) -> appointment.getRoomId() == id)){
               //doy a entender que ya existe un appointment de antes por eso no lo ocupe
                allAppointments.add(new Appointment(roomId, null, null, LocalDateTime.MIN));
           }else {
               //si no hay manera de que se ocupe una room se devuelve solo las que se pudieron llenar sin tirar excepcion
               Optional<Appointment> maybeAppointment =  findAttendanceForWaitingPatient(new Appointment(roomId, null, null, null));
               if (maybeAppointment.isEmpty()){
                   //si no se pudo asignar un doctor para los pacientes que quedan doy a entender que quedaron vacias con null en localdatetime
                   allAppointments.add(new Appointment(roomId, null, null, null));
               }else{
                   //se que va a ser una room llenada ahora por el localdatetime
                   allAppointments.add(maybeAppointment.get());
               }
           }
       }

       return allAppointments;
    }

   public Appointment dischargePatient(Appointment appointment){
        if (doctorRepository.getAllDoctors().stream().noneMatch(doctor -> doctor.equals(appointment.getDoctor()))){
            throw new DoctorNotFoundException();
        }
        if (roomsRepository.getUnavailableRooms().stream().noneMatch(app -> app.equals(appointment))){
            throw new AppointmentNotFoundException();
        }
        //debería poder eliminar el elemento por reconociendo el objeto por el equals
        roomsRepository.getUnavailableRooms().remove(appointment);
        appointment.getDoctor().setDisponibility(Disponibility.AVAILABLE);
        roomsRepository.getAvailableRooms().add(appointment.getRoomId());
        //devuelvo el objeto para que el cliente le sirva la info de que sucedio
        return appointment;
   }
    private Optional<Appointment> findAttendanceForWaitingPatient(Appointment appointment) {
        //obtengo la lista de pacientes y la recorro en busqueda de un match
        Iterator<Patient> patientIterator = patientRepository.getWaitingRoom().iterator();

        Comparator<Doctor> ascendingLevelAlphab = (d1, d2) -> {
            if (d1.getLevel().ordinal() == d2.getLevel().ordinal()){
                return d1.getDoctorName().compareTo(d2.getDoctorName());
            }
            return d1.getLevel().ordinal() - d2.getLevel().ordinal();
        };

        //armo colección auxiliar para que todos los que puedan atender queden ordenados y solo tenga que sacar el primero
        Queue<Doctor> candidates = new PriorityBlockingQueue<>(10, ascendingLevelAlphab);

        while (patientIterator.hasNext()){
            //se obtiene el nivel de risk y veo si hay alguno que matchee
            //necesito chequear que además esten available esos doctors
            Patient currentPatient = patientIterator.next();
            Level currentPatientsLevel = currentPatient.getPatientLevel();

            ArrayList<Doctor> doctorArray = doctorRepository.getAllDoctors();
            for(Doctor doctor : doctorArray){
                //todos los que cumplan y tengan el nivel más bajo (igual al del paciente)
                // van a la colección auxiliar
                if (doctor.getDisponibility() == Disponibility.AVAILABLE && doctor.getLevel().ordinal() >= currentPatientsLevel.ordinal()){
                    candidates.add(doctor);
                }
            }
            if (!candidates.isEmpty()){
                //reutilizo el objeto que recibí -> check
                appointment.setPatient(currentPatient);
                Doctor doctorMatched = candidates.poll();
                appointment.setDoctor(doctorMatched);
                appointment.setStartTime(LocalDateTime.now());
                //cambio el estado del doctor y las salas
                //TODO: testear que se modifique la instancia correcta
                doctorMatched.setDisponibility(Disponibility.ATTENDING);
                roomsRepository.getAvailableRooms().remove(appointment.getRoomId());
                roomsRepository.getUnavailableRooms().add(appointment);
                return Optional.of(appointment);
            }
        }
        return Optional.empty();
    }


}
