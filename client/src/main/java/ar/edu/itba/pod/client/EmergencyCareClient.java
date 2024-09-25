package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.AdminServiceGrpc;
import ar.edu.itba.pod.grpc.EmergencyAttentionGrpc;
import ar.edu.itba.pod.grpc.Service;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmergencyCareClient extends Client<EmergencyCareClient.EmergencyCareActions>{
    ExecutorService executorService= Executors.newSingleThreadExecutor();
    EmergencyAttentionGrpc.EmergencyAttentionFutureStub emergencyAttentionFutureStub;

    FutureCallback<Service.AllRoomsFullInfo> callbackCareAllPatients = new FutureCallback<>() {
        @Override
        public void onSuccess(Service.AllRoomsFullInfo roomsInfo) {
            List<Service.RoomFullInfo> rooms = roomsInfo.getRoomsInfoList();
            String ocuppiedMessage = "Room #%d remains Occupied";
            String freeMessage = "Room #%d remains Free";
            String placedMessage = "Patient %s (%d) and Doctor %s (%d) are now in Room #%d";
            StringBuilder fullMessage = new StringBuilder();
            for (Service.RoomFullInfo room : rooms){
                if (room.getAvailability()){
                   fullMessage.append(freeMessage.formatted(room.getId()) + '\n');
                }else if(room.getRoomInfo().getPatient().isEmpty()){
                   fullMessage.append(ocuppiedMessage.formatted(room.getId()) + '\n');
                }else{
                   fullMessage.append(placedMessage.formatted(room.getRoomInfo().getPatient(),room.getRoomInfo().getPatientLevel().getNumber(), room.getRoomInfo().getDoctor(), room.getRoomInfo().getDoctorLevel().getNumber(), room.getId()) + '\n');
                }
            }
            System.out.println(fullMessage);
            countDownLatch.countDown();
        }

        @Override
        public void onFailure(Throwable throwable) {
            System.out.println(throwable.getMessage());
            countDownLatch.countDown();
        }
    };


    public static void main(String[] args) throws Exception {
        new EmergencyCareClient().startClient();

    }
    public EmergencyCareClient(){
        actionMapper= Map.of(EmergencyCareActions.CARE_PATIENT,()->{
                    String roomParam=System.getProperty("room");
                    if(roomParam==null){
                        System.out.println("room id not specified");
                        countDownLatch.countDown();
                        return;
                    }
                    long roomId = Long.parseLong(roomParam);
                    com.google.common.util.concurrent.ListenableFuture<Service.RoomBasicInfo> listenableFuture = emergencyAttentionFutureStub.carePatient(Int64Value.newBuilder().setValue(roomId).build());
                    FutureCallback<Service.RoomBasicInfo> callbackCarePatient =new FutureCallback<>() {
                        @Override
                        public void onSuccess(Service.RoomBasicInfo roomInfo) {
                            String message = "Patient %s (%d) and Doctor %s (%d) are now in Room #%d";
                            System.out.println(message.formatted(roomInfo.getPatient(), roomInfo.getPatientLevel().getNumber(), roomInfo.getDoctor(), roomInfo.getDoctorLevel().getNumber(), roomId));
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            System.out.println(throwable.getMessage());
                            countDownLatch.countDown();
                        }
                    };
                    Futures.addCallback(listenableFuture, callbackCarePatient, executorService);
                },
                EmergencyCareActions.CARE_ALL_PATIENTS, ()->{
                    Futures.addCallback(emergencyAttentionFutureStub.careAllPatients(Empty.newBuilder().build()),callbackCareAllPatients, executorService);
                },
                EmergencyCareActions.DISCHARGE_PATIENT, ()->{
                    String roomParam=System.getProperty("room");
                    if(roomParam==null){
                        System.out.println("room id not specified");
                        countDownLatch.countDown();
                        return;
                    }
                    long roomId = Long.parseLong(roomParam);
                    String doctorName = System.getProperty("doctor");
                    String patientName = System.getProperty("patient");
                    com.google.common.util.concurrent.ListenableFuture<Service.RoomBasicInfo> listenableFuture = emergencyAttentionFutureStub.dischargePatient(Service.RoomDischargeInfo.newBuilder().setDoctor(doctorName).setPatient(patientName).setId(roomId).build());
                    FutureCallback<Service.RoomBasicInfo> callbackDischargePatient =new FutureCallback<>() {
                        @Override
                        public void onSuccess(Service.RoomBasicInfo roomInfo) {
                            String message = "Patient %s (%d) has been discharged from Doctor %s (%d) and the Room #%d is now Free";
                            System.out.println(message.formatted(roomInfo.getPatient(), roomInfo.getPatientLevel().getNumber(), roomInfo.getDoctor(), roomInfo.getDoctorLevel().getNumber(), roomId));
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            System.out.println(throwable.getMessage());
                            countDownLatch.countDown();
                        }
                    };
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
