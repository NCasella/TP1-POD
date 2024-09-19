package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.AdminServiceGrpc;
import ar.edu.itba.pod.grpc.EmergencyAttentionGrpc;
import ar.edu.itba.pod.grpc.Service;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.Empty;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmergencyCareClient extends Client<EmergencyCareClient.EmergencyCareActions>{
    ExecutorService executorService= Executors.newSingleThreadExecutor();
    EmergencyAttentionGrpc.EmergencyAttentionFutureStub emergencyAttentionFutureStub;
    FutureCallback<Service.RoomFullInfo> callbackCarePatient =new FutureCallback<>() {
        @Override
        public void onSuccess(Service.RoomFullInfo roomInfo) {
            String message = "Patient %s (%d) and Doctor %s (%d) are now in Room #%d";
            System.out.println(message.formatted(roomInfo.getPatient(), roomInfo.getPatientLevel().getNumber(), roomInfo.getDoctor(), roomInfo.getDoctorLevel().getNumber(), roomInfo.getAvailability().getId()));
            countDownLatch.countDown();
        }

        @Override
        public void onFailure(Throwable throwable) {
            System.out.println(throwable.getMessage());
        }
    };

    FutureCallback<Service.RoomFullInfo> callbackDischargePatient =new FutureCallback<>() {
        @Override
        public void onSuccess(Service.RoomFullInfo roomInfo) {
            String message = "Patient %s (%d) has been discharged from Doctor %s (%d) and the Room #%d is now Free";
            System.out.println(message.formatted(roomInfo.getPatient(), roomInfo.getPatientLevel().getNumber(), roomInfo.getDoctor(), roomInfo.getDoctorLevel().getNumber(), roomInfo.getAvailability().getId()));
            countDownLatch.countDown();
        }

        @Override
        public void onFailure(Throwable throwable) {
            System.out.println(throwable.getMessage());
        }
    };

    FutureCallback<Service.AllRoomsFullInfo> callbackCareAllPatients = new FutureCallback<>() {
        @Override
        public void onSuccess(Service.AllRoomsFullInfo roomsInfo) {
            List<Service.RoomFullInfo> rooms = roomsInfo.getRoomsInfoList();
            String ocuppiedMessage = "Room #%d remains Occupied";
            String freeMessage = "Room #%d remains Free";
            String placedMessage = "Patient %s (%d) and Doctor %s (%d) are now in Room #%d";
            StringBuilder fullMessage = new StringBuilder();
            for (Service.RoomFullInfo room : rooms){
                if (room.getAvailability().getAvailability()){
                   fullMessage.append(freeMessage.formatted(room.getAvailability().getId()) + '\n');
                }else if(room.getPatient().isEmpty()){
                   fullMessage.append(ocuppiedMessage.formatted(room.getAvailability().getId()) + '\n');
                }else{
                   fullMessage.append(placedMessage.formatted(room.getPatient(),room.getPatientLevel().getNumber(), room.getDoctor(), room.getDoctorLevel().getNumber(), room.getAvailability().getId()) + '\n');
                }
            }
            System.out.println(fullMessage);
            countDownLatch.countDown();
        }

        @Override
        public void onFailure(Throwable throwable) {
            System.out.println(throwable.getMessage());
        }
    };


    public static void main(String[] args) throws Exception {
        new EmergencyCareClient().startClient();

    }
    public EmergencyCareClient(){
        actionMapper= Map.of(EmergencyCareActions.CARE_PATIENT,()->{
                    Long roomId = Long.valueOf(System.getProperty("room"));
                    com.google.common.util.concurrent.ListenableFuture<Service.RoomFullInfo> listenableFuture = emergencyAttentionFutureStub.carePatient(Service.RoomBasicInfo.newBuilder().setId(roomId).build());
                    Futures.addCallback(listenableFuture, callbackCarePatient, executorService);
                },
                EmergencyCareActions.CARE_ALL_PATIENTS, ()->{
                    Futures.addCallback(emergencyAttentionFutureStub.careAllPatients(Empty.newBuilder().build()),callbackCareAllPatients, executorService);
                },
                EmergencyCareActions.DISCHARGE_PATIENT, ()->{
                    Long roomId = Long.valueOf(System.getProperty("room"));
                    String doctorName = System.getProperty("doctor");
                    String patientName = System.getProperty("patient");
                    com.google.common.util.concurrent.ListenableFuture<Service.RoomFullInfo> listenableFuture = emergencyAttentionFutureStub.dischargePatient(Service.RoomFullInfo.newBuilder().setDoctor(doctorName).setPatient(patientName).setAvailability(Service.RoomBasicInfo.newBuilder().setId(roomId).build()).build());
                    Futures.addCallback(listenableFuture, callbackDischargePatient, executorService);
                }
        );
    }

    @Override
    protected void runClientCode() throws InterruptedException{
        emergencyAttentionFutureStub = EmergencyAttentionGrpc.newFutureStub(channel);
        actionMapper.get(actionProperty).run();
        countDownLatch.await();
        executorService.shutdown();
    }

    @Override
    protected Class<EmergencyCareActions> getEnumClass() {
        return EmergencyCareActions.class;
    }

    protected enum EmergencyCareActions{
        CARE_PATIENT("carePatient"),
        CARE_ALL_PATIENTS("careAllPatients"),
        DISCHARGE_PATIENT("dischargePatient");

        private final String paramName;

        EmergencyCareActions(String paramName){
            this.paramName=paramName;
        }

        @Override
        public String toString(){
            return this.paramName;
        }
    }

}
