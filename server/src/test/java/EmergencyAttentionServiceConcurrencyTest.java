import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.server.repositories.*;
import ar.edu.itba.pod.server.services.AdminService;
import ar.edu.itba.pod.server.services.EmergencyAttentionServiceImpl;
import ar.edu.itba.pod.server.services.WaitingRoomServiceImpl;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.UInt64Value;
import io.grpc.stub.StreamObserver;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class EmergencyAttentionServiceConcurrencyTest {
    private final RoomRepository roomsRepository = new RoomRepository();
    private final PatientRepository patientRepository = new PatientRepository();
    private final DoctorRepository doctorRepository = new DoctorRepository();
    private static final int BASICNUMTHREADS= 1000;
    private final FinishedAppointmentRepository finishedAppointmentRepository = new FinishedAppointmentRepository();
    private final NotificationRepository notificationRepository = new NotificationRepository(doctorRepository);

    private final EmergencyAttentionServiceImpl emergencyAttentionService = new EmergencyAttentionServiceImpl(patientRepository, doctorRepository,roomsRepository,notificationRepository,finishedAppointmentRepository);
    private final AdminService adminService = new AdminService(doctorRepository,roomsRepository,notificationRepository);
    private final WaitingRoomServiceImpl waitingRoomService = new WaitingRoomServiceImpl(patientRepository);
    private final StreamObserver<Service.RoomBasicInfo> streamObserverRoomBasicInfo = new StreamObserver<Service.RoomBasicInfo>() {
        @Override
        public void onNext(Service.RoomBasicInfo roomBasicInfo) {

        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onCompleted() {

        }
    };
    private final StreamObserver<UInt64Value> streamObserverLong = new StreamObserver<UInt64Value>() {
        @Override
        public void onNext(UInt64Value uInt64Value) {

        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onCompleted() {

        }
    };

    private final StreamObserver<Service.EnrollmentInfo> enrollmentInfoStreamObserver = new StreamObserver<Service.EnrollmentInfo>() {
        @Override
        public void onNext(Service.EnrollmentInfo enrollmentInfo) {

        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onCompleted() {

        }
    };

    private final StreamObserver<Service.CompleteDoctorAvailabilityInfo> completeDoctorAvailabilityInfoStreamObserver = new StreamObserver<Service.CompleteDoctorAvailabilityInfo>() {
        @Override
        public void onNext(Service.CompleteDoctorAvailabilityInfo completeDoctorAvailabilityInfo) {

        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onCompleted() {

        }
    };

    private void prepareRooms(){
        ExecutorService executor = Executors.newFixedThreadPool(BASICNUMTHREADS);
        List<Callable<Boolean>> operations = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(BASICNUMTHREADS);
        for (int currentNum = 0; currentNum < BASICNUMTHREADS; currentNum++) {
            operations.add(() -> {
                try {
                    adminService.addRoom(Empty.getDefaultInstance(),streamObserverLong);
                    return true;
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        try {
            List<Future<Boolean>> result =  executor.invokeAll(operations);
            countDownLatch.await();
            for (Future<Boolean> future : result){
                Boolean finalValue = future.get();
                if (!finalValue){
                    System.out.println("Something went wrong during a single thread execution...");
                }
            }

        }catch (InterruptedException e){
            System.out.println("Interrupted Exception was caught, something went wrong...");
        } catch (ExecutionException e) {
            System.out.println("Execution Exception was caught, something went wrong during computation...");
            throw new RuntimeException(e);
        }
    }

    private void prepareDoctors(){
            ExecutorService executor = Executors.newFixedThreadPool(BASICNUMTHREADS);
            List<Callable<Boolean>> operations = new ArrayList<>();
            CountDownLatch countDownLatch = new CountDownLatch(BASICNUMTHREADS);
            for (int currentNum = 0; currentNum < BASICNUMTHREADS; currentNum++) {
                int finalCurrentNum = currentNum;
                operations.add(() -> {
                    try {
                        Service.EnrollmentInfo enrollmentInfo = Service.EnrollmentInfo.newBuilder().setName("doctor"+ finalCurrentNum).setLevel(Service.Level.LEVEL_1).build();
                        adminService.addDoctor(enrollmentInfo, enrollmentInfoStreamObserver);
                        Service.DoctorAvailabilityRequest doctorAvailabilityRequest = Service.DoctorAvailabilityRequest.newBuilder().setDoctorName("doctor" + finalCurrentNum).setDoctorAvailability(Service.Availability.DOCTOR_AVAILABLE).build();
                        adminService.setDoctor(doctorAvailabilityRequest, completeDoctorAvailabilityInfoStreamObserver);
                        return true;
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }
            try {
                List<Future<Boolean>> result =  executor.invokeAll(operations);
                countDownLatch.await();
                for (Future<Boolean> future : result){
                    Boolean finalValue = future.get();
                    if (!finalValue){
                        System.out.println("Something went wrong during a single thread execution...");
                    }
                }

            }catch (InterruptedException e){
                System.out.println("Interrupted Exception was caught, something went wrong...");
            } catch (ExecutionException e) {
                System.out.println("Execution Exception was caught, something went wrong during computation...");
                throw new RuntimeException(e);
            }
    }

    private void preparePatients(){
        ExecutorService executor = Executors.newFixedThreadPool(BASICNUMTHREADS);
        List<Callable<Boolean>> operations = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(BASICNUMTHREADS);
        for (int currentNum = 0; currentNum < BASICNUMTHREADS; currentNum++) {
            int finalCurrentNum = currentNum;
            operations.add(() -> {
                try {
                    Service.EnrollmentInfo enrollmentInfo = Service.EnrollmentInfo.newBuilder().setName("patient"+ finalCurrentNum).setLevel(Service.Level.LEVEL_1).build();
                    waitingRoomService.addPatient(enrollmentInfo, enrollmentInfoStreamObserver);
                    return true;
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        try {
            List<Future<Boolean>> result =  executor.invokeAll(operations);
            countDownLatch.await();
            for (Future<Boolean> future : result){
                Boolean finalValue = future.get();
                if (!finalValue){
                    System.out.println("Something went wrong during a single thread execution...");
                }
            }

        }catch (InterruptedException e){
            System.out.println("Interrupted Exception was caught, something went wrong...");
        } catch (ExecutionException e) {
            System.out.println("Execution Exception was caught, something went wrong during computation...");
            throw new RuntimeException(e);
        }
    }

    public void prepareTest(){
        prepareRooms();
        preparePatients();
        prepareDoctors();
    }

    @Test
    public void carePatientTest(){
        prepareTest();
        ExecutorService executor = Executors.newFixedThreadPool(BASICNUMTHREADS);
        List<Callable<Boolean>> operations = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(BASICNUMTHREADS);
        for (int currentNum = 1; currentNum <= BASICNUMTHREADS; currentNum++) {
            Int64Value room = Int64Value.newBuilder().setValue((long) currentNum).build();
            operations.add(() -> {
                try {
                    emergencyAttentionService.carePatient(room, streamObserverRoomBasicInfo);
                    return true;
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        try {
            List<Future<Boolean>> result =  executor.invokeAll(operations);
            countDownLatch.await();
            for (Future<Boolean> future : result){
                Boolean finalValue = future.get();
                if (!finalValue){
                    System.out.println("Something went wrong during a single thread execution...");
                }
            }

        }catch (InterruptedException e){
            System.out.println("Interrupted Exception was caught, something went wrong...");
        } catch (ExecutionException e) {
            System.out.println("Execution Exception was caught, something went wrong during computation...");
            throw new RuntimeException(e);
        }
    }

}
