package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.AdminServiceGrpc;
import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.grpc.WaitingRoomGrpc;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WaitingRoomClient extends Client<WaitingRoomClient.WaitingRoomActions> {

    ExecutorService executorService= Executors.newSingleThreadExecutor();
    WaitingRoomGrpc.WaitingRoomFutureStub waitingRoomBlockingStub;
    FutureCallback<Service.EnrollmentInfo> addPatientAndUpdateLevelCallback=new FutureCallback<>() {
        @Override
        public void onSuccess(Service.EnrollmentInfo enrollmentInfo) {
            String message = "Patient %s (%d) is in the waiting room";
            System.out.println(message.formatted(enrollmentInfo.getName(), enrollmentInfo.getLevel().getNumber()));
            countDownLatch.countDown();
        }

        @Override
        public void onFailure(Throwable throwable) {
            System.out.println(throwable.getMessage());
            countDownLatch.countDown();
        }
    };



    public static void main(String[] args) throws Exception {
        new WaitingRoomClient().startClient();

    }

    private void exitThread(String message){
        System.out.println(message);
        countDownLatch.countDown();
    }

    public WaitingRoomClient(){
        actionMapper= Map.of(WaitingRoomActions.ADD_PATIENT,()->{
                    String name=System.getProperty("patient");
                    String level = System.getProperty("level");
                    if ( name==null || level==null ){
                        exitThread("Missing arguments");
                        return;
                    }
                    int levelIndex;
                    try{
                        levelIndex=Integer.parseInt(level);
                    }
                    catch (NumberFormatException e){
                        exitThread("Invalid level parameter");
                        return;
                    }
                    int length = Service.Level.values().length;
                    if ( levelIndex < Service.Level.LEVEL_1_VALUE || length-2 < levelIndex) {
                        exitThread("Invalid level parameter");
                        return;
                    }
                    ListenableFuture<Service.EnrollmentInfo> listenableFuture = waitingRoomBlockingStub.addPatient(Service.EnrollmentInfo.newBuilder().setName(name).setLevel(Service.Level.forNumber(levelIndex)).build());
                    Futures.addCallback(listenableFuture, addPatientAndUpdateLevelCallback,executorService);
                },
                WaitingRoomActions.UPDATE_LEVEL,()->{
                    String name =System.getProperty("patient");
                    int levelIndex =Integer.parseInt(System.getProperty("level"));
                    ListenableFuture<Service.EnrollmentInfo> listenableFuture = waitingRoomBlockingStub.updatePatientLevel(Service.EnrollmentInfo.newBuilder().setName(name).setLevel(Service.Level.forNumber(levelIndex)).build());
                    Futures.addCallback(listenableFuture, addPatientAndUpdateLevelCallback,executorService);
                },
                WaitingRoomActions.CHECK_PATIENT,()->{
                    String name =System.getProperty("patient");
                    FutureCallback<Service.PatientsAhead> updateLevelCallback=new FutureCallback<>() {
                        @Override
                        public void onSuccess(Service.PatientsAhead patientsAhead) {
                            String message = "Patient %s (%d) is in the waiting room with %d patients ahead";
                            System.out.println(message.formatted(name,patientsAhead.getPatientLevel().getNumber(), patientsAhead.getPatients()));
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            System.out.println(throwable.getMessage());
                            countDownLatch.countDown();
                        }
                    };

                    Futures.addCallback(waitingRoomBlockingStub.getPatientsAhead(StringValue.newBuilder().setValue(name).build()),updateLevelCallback,executorService);
                }
        );
    }

    @Override
    protected void runClientCode() throws InterruptedException{
        waitingRoomBlockingStub = WaitingRoomGrpc.newFutureStub(channel);
        actionMapper.get(actionProperty).run();
        countDownLatch.await();
        executorService.shutdown();
    }

    @Override
    protected Class<WaitingRoomActions> getEnumClass() {
        return WaitingRoomActions.class;
    }

    protected enum WaitingRoomActions{
        ADD_PATIENT("addPatient"),
        UPDATE_LEVEL("updateLevel"),
        CHECK_PATIENT("checkPatient");

        private final String paramName;

        WaitingRoomActions(String paramName){
            this.paramName=paramName;
        }

        @Override
        public String toString(){
            return this.paramName;
        }
    }

}
