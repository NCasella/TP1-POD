package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.AdminServiceGrpc;
import ar.edu.itba.pod.grpc.Service;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;



import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;

public class AdminServiceClient extends Client<AdminServiceClient.AdminActions> {
    ExecutorService executorService= Executors.newSingleThreadExecutor();
    AdminServiceGrpc.AdminServiceFutureStub adminServiceStub;
    FutureCallback<Service.CompleteDoctorAvailabilityInfo> disponibilityCallback=new FutureCallback<>() {
        @Override
        public void onSuccess(Service.CompleteDoctorAvailabilityInfo value) {
            System.out.printf("Doctor %s (%d) is %s",value.getDoctorName(),value.getDoctorLevel().getNumber(), getStringFromAvailability(value.getDoctorAvailability()));
            countDownLatch.countDown();
        }

        @Override
        public void onFailure(Throwable throwable) {
            System.out.println(throwable.getMessage());
            countDownLatch.countDown();
        }
    };

    private static String getStringFromAvailability(Service.Availability availability){
        Map<Service.Availability,String> availabilityStringMap=Map.of(
                Service.Availability.DOCTOR_AVAILABLE,"Available",
                Service.Availability.DOCTOR_UNAVAILABLE,"Unavailable",
                Service.Availability.DOCTOR_ATTENDING,"Attending",
                Service.Availability.UNRECOGNIZED,"Status unknown"
                );
        return availabilityStringMap.get(availability);
    }
    public static void main(String[] args) throws Exception {
        new AdminServiceClient().startClient();
    }

    public AdminServiceClient(){
        actionMapper= Map.of(AdminActions.ADD_DOCTOR,()->{
            String name=System.getProperty("doctor");
            String level=System.getProperty("level");
            if(level==null || name==null){
                System.out.println("missing parameters for doctor addition");
                countDownLatch.countDown();
                return;
            }
            int levelIndex;
            try{
                levelIndex=Integer.parseInt(level);
            }
            catch (NumberFormatException e){
                System.out.println("invalid level parameter");
                countDownLatch.countDown();
                return;
            }
            ListenableFuture<Service.EnrollmentInfo> listenableFuture = adminServiceStub.addDoctor(Service.EnrollmentInfo.newBuilder().setName(name).setLevel(Service.Level.forNumber(levelIndex)).build());
            Futures.addCallback(listenableFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(Service.EnrollmentInfo enrollmentInfo) {
                    System.out.printf("Doctor %s (%d) added successfully",enrollmentInfo.getName(),enrollmentInfo.getLevelValue());
                    countDownLatch.countDown();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    System.out.println(throwable.getMessage());
                    countDownLatch.countDown();
                }
            }, executorService);
            },
                AdminActions.CHECK_DOCTOR,()->{
            String doctor=System.getProperty("doctor");
            Futures.addCallback(adminServiceStub.checkDoctor(StringValue.of(doctor)),disponibilityCallback,executorService);
        },
           AdminActions.SET_DOCTOR,()->{
            String doctor=System.getProperty("doctor");
            String availabilityParam=System.getProperty("availability");

            Service.Availability availability= Arrays.stream(Service.Availability.values())
                    .filter((arrValue)->arrValue.getValueDescriptor().getOptions().getExtension(Service.availabilityValue).equals(availabilityParam)).findAny().orElseThrow(IllegalArgumentException::new);
            Futures.addCallback(adminServiceStub.setDoctor(Service.DoctorAvailabilityRequest.newBuilder().setDoctorName(doctor).setDoctorAvailability(availability).build()),disponibilityCallback,executorService);
                },
                AdminActions.ADD_ROOM,()-> Futures.addCallback(adminServiceStub.addRoom(Empty.newBuilder().build()), new FutureCallback<>() {
                    @Override
                    public void onSuccess(UInt64Value uInt64Value) {
                        System.out.printf("Room #%d added successfully",uInt64Value.getValue());
                        countDownLatch.countDown();
                    }
                    @Override
                    public void onFailure(Throwable throwable) {
                        System.out.println(throwable.getMessage());
                        countDownLatch.countDown();
                    }
                }, executorService)
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
