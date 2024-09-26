import ar.edu.itba.pod.server.models.Level;
import ar.edu.itba.pod.server.models.Patient;
import ar.edu.itba.pod.server.repositories.DoctorRepository;
import ar.edu.itba.pod.server.repositories.PatientRepository;
import ar.edu.itba.pod.server.repositories.RoomRepository;
import ar.edu.itba.pod.server.services.WaitingRoomServiceImpl;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

public class WaitingRoomServiceTest {
    private final RoomRepository roomRepository = new RoomRepository();
    private final DoctorRepository doctorRepository = new DoctorRepository();
    private final PatientRepository patientRepository = new PatientRepository();

    private final List<Patient> waitingRoom = new ArrayList<>();
    private final WaitingRoomServiceImpl waitingRoomService = new WaitingRoomServiceImpl(patientRepository);

    private final static int ROOMS = 5;

    private void addPatientsDoctorsRooms() {
        for (int i = 0; i < ROOMS; i++) {
            roomRepository.addRoom();
        }
        try {
            patientRepository.addpatient("Lucas", Level.LEVEL_5);
            patientRepository.addpatient("Foo", Level.LEVEL_3);
            patientRepository.addpatient("James", Level.LEVEL_3);
            patientRepository.addpatient("Carlin", Level.LEVEL_1);
        } catch (Exception e) {}

        //al debuggear se ve que al bypassear todos los pasos añadiendo directamente los pacientes desde el repositorio se termina guardando el mismo arrival time por eso fallaba con el supuesto inicial
        doctorRepository.addDoctor("L1",Level.LEVEL_1);
        doctorRepository.addDoctor("L2",Level.LEVEL_2);
        doctorRepository.addDoctor("L3",Level.LEVEL_3);
        doctorRepository.addDoctor("L4",Level.LEVEL_4);
        doctorRepository.addDoctor("L5",Level.LEVEL_5);


    }

    @Test
    public void getPatientsAhead() {
        addPatientsDoctorsRooms();


        String patientName = "James";
        Patient patient= patientRepository.getPatientFromName(patientName);
        Assertions.assertEquals(2,patientRepository.getPatientsAhead(patient).getFirst());

        try {
            patientRepository.setPatientLevel(patient,Level.LEVEL_4);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertEquals(1,patientRepository.getPatientsAhead(patient).getFirst());

        for (Patient p : patientRepository.getWaitingRoomList())
            System.out.println(p.getPatientName());

        patientName = "Foo";
        patient= patientRepository.getPatientFromName(patientName);
        Assertions.assertEquals(2,patientRepository.getPatientsAhead(patient).getFirst());

        try {
            patientRepository.setPatientLevel(patient,Level.LEVEL_4);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (Patient p : patientRepository.getWaitingRoomList())
            System.out.println(p.getPatientName()+" level="+p.getPatientLevel() );
        Assertions.assertEquals(2,patientRepository.getPatientsAhead(patient).getFirst());

        patientName = "James";
        patient= patientRepository.getPatientFromName(patientName);
        Assertions.assertEquals(1,patientRepository.getPatientsAhead(patient).getFirst());

        patientName = "Carlin";
        patient= patientRepository.getPatientFromName(patientName);
        Assertions.assertEquals(3,patientRepository.getPatientsAhead(patient).getFirst());
    }
}
