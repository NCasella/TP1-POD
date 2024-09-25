package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.QueryMakerGrpc;
import ar.edu.itba.pod.grpc.Service;
import com.google.protobuf.Empty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class QueryClient extends Client<QueryClient.QueryActions>{

    private QueryMakerGrpc.QueryMakerBlockingStub stub;

    public QueryClient() {
        actionMapper= Map.of(QueryActions.QUERY_ROOMS,()->{
            String filename=System.getProperty("outPath");
            if(filename==null){
                System.out.println("filename not specified");
                return;
            }

         Service.RoomsCurrentState roomsCurrentState= stub.queryRooms(Empty.newBuilder().build());
            Path path=Paths.get(filename);
            try {
                Files.write(path,"Room,Status,Patient,Doctor\n".getBytes());
            } catch (IOException e) {
                System.out.println("Error writing to file");
                return;
            }
            for(Service.RoomFullInfo room: roomsCurrentState.getRoomsList()){
                StringBuilder stringToWrite=new StringBuilder().append(room.getId());
                Service.RoomBasicInfo roomInfo=room.getRoomInfo();
                if(room.getAvailability()){
                    stringToWrite.append(",Free,,\n");
                }
                else{
                    stringToWrite.append(",Occupied,").append(roomInfo.getPatient())
                            .append(" (").append(roomInfo.getPatientLevelValue()).append("),")
                            .append(roomInfo.getDoctor()).append(" (").append(roomInfo.getDoctorLevelValue()).append(")\n");
                }
                try {
                    Files.write(path,stringToWrite.toString().getBytes(),StandardOpenOption.APPEND);
                } catch (IOException e) {
                    System.out.println("Error writing to file");
                    return;
                }
            }

        },
        QueryActions.QUERY_WAITING_ROOM, () -> {
          String filename=System.getProperty("outPath");
          if(filename==null){
              System.out.println("filename not specified");
              return;
          }
          Service.PatientsCurrentState patientsCurrentState = stub.queryWaitingRooms(Empty.newBuilder().build());
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
                Service.FinishedAppointmentsState finishedAppointmentsState = stub.queryCares(queryBuilder.build());
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
        } );
    }

    @Override
    protected void runClientCode() throws InterruptedException {
        stub= QueryMakerGrpc.newBlockingStub(channel);
        actionMapper.get(actionProperty).run();

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
