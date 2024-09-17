package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.AdminServiceGrpc;
import ar.edu.itba.pod.grpc.Service;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;

public class AdminServiceClient extends Client<AdminServiceClient.AdminActions> {
    ExecutorService executorService= Executors.newSingleThreadExecutor();
    AdminServiceGrpc.AdminServiceFutureStub adminServiceStub;
    FutureCallback<StringValue> callback=new FutureCallback<>() {
        @Override
        public void onSuccess(StringValue stringValue) {
            System.out.println(stringValue);
            countDownLatch.countDown();
        }

        @Override
        public void onFailure(Throwable throwable) {
            System.out.println(throwable.getMessage());
        }
    };

    public static void main(String[] args) throws Exception {
        new AdminServiceClient().startClient();

    }
    public AdminServiceClient(){
        actionMapper= Map.of(AdminActions.ADD_DOCTOR,()->{
            String name=System.getProperty("doctor");
            int levelIndex=Integer.parseInt(System.getProperty("level"));
            if(levelIndex>5) {
                System.out.println("invalid parameter");
                return;
            }
            ListenableFuture<StringValue> listenableFuture = adminServiceStub.addDoctor(Service.EnrollmentInfo.newBuilder().setName(name).setLevel(Service.Level.forNumber(levelIndex)).build());
            Futures.addCallback(listenableFuture, callback,executorService);
            },
                AdminActions.CHECK_DOCTOR,()->{
            String doctor=System.getProperty("doctor");
            Futures.addCallback(adminServiceStub.checkDoctor(StringValue.of(doctor)),callback,executorService);
        },
           AdminActions.SET_DOCTOR,()->{
            String doctor=System.getProperty("doctor");
            String availabilityParam=System.getProperty("availability");
            Service.Availability availability= Arrays.stream(Service.Availability.values())
                    .filter((arrValue)->arrValue.getValueDescriptor().getOptions().getExtension(Service.availabilityValue).equals(availabilityParam)).findAny().orElseThrow(IllegalArgumentException::new);
            Futures.addCallback(adminServiceStub.setDoctor(Service.DoctorAvailabilityInfo.newBuilder().setName(doctor).setAvailability(availability).build()),callback,executorService);
                },
                AdminActions.ADD_ROOM,()-> Futures.addCallback(adminServiceStub.addRoom(Empty.newBuilder().build()),callback,executorService)
                );
    }

    @Override
    protected void runClientCode() throws InterruptedException{
        adminServiceStub = AdminServiceGrpc.newFutureStub(channel);
        actionMapper.get(actionProperty).run();
        countDownLatch.await();
        executorService.shutdown();
    }

    @Override
    protected Class<AdminActions> getEnumClass() {
        return AdminActions.class;
    }

    protected enum AdminActions{
        ADD_ROOM("addRoom"),
        ADD_DOCTOR("addDoctor"),
        SET_DOCTOR("setDoctor"),
        CHECK_DOCTOR("checkDoctor");

        private final String paramName;

        AdminActions(String paramName){
            this.paramName=paramName;
        }

        @Override
        public String toString(){
            return this.paramName;
        }
    }
}
