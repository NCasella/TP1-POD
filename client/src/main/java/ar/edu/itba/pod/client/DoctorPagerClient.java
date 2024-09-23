package ar.edu.itba.pod.client;


import ar.edu.itba.pod.grpc.DoctorPagerGrpc;
import ar.edu.itba.pod.grpc.EmergencyAttentionGrpc;
import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.models.ActionType;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DoctorPagerClient extends Client<DoctorPagerClient.DoctorPagerActions> {
    ExecutorService executorService= Executors.newSingleThreadExecutor();
    DoctorPagerGrpc.DoctorPagerFutureStub doctorPagerFutureStub;
    DoctorPagerGrpc.DoctorPagerStub doctorPagerStub;
    String doctorName;

    StreamObserver<Service.Notification> registerResponseObserver = new StreamObserver<>() {
        @Override
        public void onNext(Service.Notification notification) {
            ActionType actionType = ActionType.getAction(notification);
            System.out.println(actionType.toString(doctorName, notification));
        }

        @Override
        public void onError(Throwable throwable) {
            System.out.println(throwable.getMessage());
            countDownLatch.countDown();
        }

        @Override
        public void onCompleted() {
            System.out.println("Closing connection...");
            countDownLatch.countDown();
        }
    };

    FutureCallback<Service.Notification> callbackUnregister = new FutureCallback<>() {
        @Override
        public void onSuccess(Service.Notification notification) {
            System.out.println(ActionType.UNREGISTER.toString( doctorName,notification));
            countDownLatch.countDown();
        }

        @Override
        public void onFailure(Throwable throwable) {
            System.out.println(throwable.getMessage());
            countDownLatch.countDown();
        }
    };


    public static void main(String[] args) throws Exception {
        new DoctorPagerClient().startClient();

    }
    public DoctorPagerClient(){
        actionMapper= Map.of(
                DoctorPagerActions.UNREGISTER, ()->{
                    Futures.addCallback(doctorPagerFutureStub.cancelNotifications(StringValue.of(doctorName)),callbackUnregister, executorService);
                },
                DoctorPagerActions.REGISTER, ()->{
                    doctorPagerStub.getNotifications(StringValue.of(doctorName),registerResponseObserver);
                }
        );
    }

    @Override
    protected void runClientCode() throws InterruptedException{
        doctorPagerFutureStub = DoctorPagerGrpc.newFutureStub(channel);
        doctorPagerStub = DoctorPagerGrpc.newStub(channel);
        doctorName = System.getProperty("doctor");
        if (doctorName == null) {
            System.out.println("You must specify a doctor name");
        } else
            actionMapper.get(actionProperty).run();
        countDownLatch.await();
        executorService.shutdown();
    }

    @Override
    protected Class<DoctorPagerClient.DoctorPagerActions> getEnumClass() {
        return DoctorPagerClient.DoctorPagerActions.class;
    }

    protected enum DoctorPagerActions {
        REGISTER("register"),
        UNREGISTER("unregister");

        private final String paramName;

        DoctorPagerActions(String paramName){
            this.paramName=paramName;
        }

        @Override
        public String toString(){
            return this.paramName;
        }
    }

}



