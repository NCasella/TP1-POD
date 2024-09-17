package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.AdminServiceGrpc;
import ar.edu.itba.pod.grpc.Service;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class AdminServiceClient extends Client<AdminServiceClient.AdminActions> {
    ExecutorService executorService= Executors.newSingleThreadExecutor();

    public static void main(String[] args) throws Exception {
        new AdminServiceClient().startClient();
    }

    @Override
    protected void runClientCode() {
        AdminServiceGrpc.AdminServiceFutureStub adminServiceStub = AdminServiceGrpc.newFutureStub(channel);
        if(actionProperty.equals(AdminActions.ADD_DOCTOR)){
            String name=System.getProperty("doctor");
            int levelIndex=Integer.parseInt(System.getProperty("level"));
            if(levelIndex>5) {
                System.out.println("invalid parameter");
                return;
            }
            ListenableFuture<StringValue> listenableFuture = adminServiceStub.addDoctor(Service.EnrollmentInfo.newBuilder().setName(name).setLevel(Service.Level.forNumber(levelIndex)).build());
            Futures.addCallback(listenableFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(StringValue stringValue) {
                    System.out.println(stringValue);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    System.out.println(throwable.getMessage());
                }
            },executorService);
        }
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
