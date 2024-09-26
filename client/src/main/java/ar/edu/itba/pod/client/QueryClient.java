package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.QueryMakerGrpc;
import ar.edu.itba.pod.grpc.Service;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueryClient extends Client<QueryClient.QueryActions>{

    private QueryMakerGrpc.QueryMakerFutureStub stub;
    private final ExecutorService executorService= Executors.newSingleThreadExecutor();
    public QueryClient() {
        actionMapper= Map.of(QueryActions.QUERY_ROOMS,()->{
            String filename=System.getProperty("outPath");
            if(filename==null){
                System.out.println("filename not specified");
                return;
            }

         ListenableFuture<Service.RoomsCurrentState> roomsFuture= stub.queryRooms(Empty.newBuilder().build());
                    Futures.addCallback(roomsFuture,getPrintStreamObserver((roomsCurrentState)->{
            if ( roomsCurrentState.getRoomsList().isEmpty())
                return;
            Path path=Paths.get(filename);
            try {
                Files.write(path,"Room,Status,Patient,Doctor\n".getBytes());
            } catch (IOException e) {
                System.out.println("Error writing to file");
                return;
            }
            for(Service.RoomFullInfo room: roomsCurrentState.getRoomsList()) {
                StringBuilder stringToWrite = new StringBuilder().append(room.getId());
                Service.RoomBasicInfo roomInfo = room.getRoomInfo();
                if (room.getAvailability()) {
                    stringToWrite.append(",Free,,\n");
                } else {
                    stringToWrite.append(",Occupied,").append(roomInfo.getPatient())
                            .append(" (").append(roomInfo.getPatientLevelValue()).append("),")
                            .append(roomInfo.getDoctor()).append(" (").append(roomInfo.getDoctorLevelValue()).append(")\n");
                }
                try {
                    Files.write(path, stringToWrite.toString().getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    System.out.println("Error writing to file");
                    return;
                }
            }
            }),executorService);

        },
        QueryActions.QUERY_WAITING_ROOM, () -> {
          String filename=System.getProperty("outPath");
          if(filename==null){
              System.out.println("filename not specified");
              return;
          }
          ListenableFuture<Service.PatientsCurrentState> patientsFuture = stub.queryWaitingRooms(Empty.newBuilder().build());
          Futures.addCallback(patientsFuture,getPrintStreamObserver((patientsCurrentState)->{
          if ( patientsCurrentState.getPatientsList().isEmpty() )
              return;

          Path path = Paths.get(filename);
          try {
              Files.write(path,"Patient,Level\n".getBytes());
          } catch (IOException e) {
              System.out.println("Error writing to file");
              return;
          }
          for(Service.PatientQueryInfo patient : patientsCurrentState.getPatientsList()){
              try{
                  StringBuilder textline = new StringBuilder();
                  textline.append(patient.getPatient()).append(",").append(patient.getLevel().getNumber()).append("\n");
                  Files.write(path,textline.toString().getBytes(), StandardOpenOption.APPEND);
              }catch(IOException e){
                  System.out.println("Error while writing in the file");
                  return;
              }
          }
          }),executorService);
          },
        QueryActions.QUERY_CARES, () ->{
                String filename=System.getProperty("outPath");
                String filterId = System.getProperty("room");
                if(filename==null){
                    System.out.println("filename not specified");
                    return;
                }
                Service.Query.Builder queryBuilder = Service.Query.newBuilder();
                if (filterId != null){
                    queryBuilder.setRoomIdFilter(Integer.parseInt(filterId));
                }
                ListenableFuture<Service.FinishedAppointmentsState> finishedAppointmentsFuture = stub.queryCares(queryBuilder.build());
                Futures.addCallback(finishedAppointmentsFuture,getPrintStreamObserver((finishedAppointmentsState)->{
                if ( finishedAppointmentsState.getAppointmentsList().isEmpty())
                    return;
                Path path = Paths.get(filename);
                try {
                    Files.write(path,"Room,Patient,Doctor\n".getBytes());
                } catch (IOException e) {
                    System.out.println("Error writing to file");
                    return;
                }
                for(Service.FinishedAppointmentQueryInfo appointment : finishedAppointmentsState.getAppointmentsList()){
                    try{
                        StringBuilder textline = new StringBuilder();
                        textline.append(appointment.getId()).append(",").append(appointment.getRoomInfo().getPatient()).append(" (").append(appointment.getRoomInfo().getPatientLevel().getNumber()).append("),").append(appointment.getRoomInfo().getDoctor()).append(" (").append(appointment.getRoomInfo().getDoctorLevel().getNumber()).append(")").append("\n");
                        Files.write(path,textline.toString().getBytes(), StandardOpenOption.APPEND);
                    }catch(IOException e){
                        System.out.println("Error while writing in the file");
                        return;
                    }
                }
        }),executorService );
    });
    }

    @Override
    protected void runClientCode() throws InterruptedException {
        stub= QueryMakerGrpc.newFutureStub(channel);
        actionMapper.get(actionProperty).run();
        countDownLatch.await();
        executorService.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {
        new QueryClient().startClient();
    }



    @Override
    protected Class<QueryActions> getEnumClass() {
        return QueryActions.class;
    }
    protected enum QueryActions{
        QUERY_WAITING_ROOM("queryWaitingRoom"),
        QUERY_ROOMS("queryRooms"),
        QUERY_CARES("queryCares");

        private final String paramName;

        QueryActions(String paramName){this.paramName=paramName;}

        @Override
        public String toString(){
            return this.paramName;
        }

    }
}
