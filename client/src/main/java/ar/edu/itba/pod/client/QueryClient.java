package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.QueryMakerGrpc;
import ar.edu.itba.pod.grpc.Service;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class QueryClient extends Client<QueryClient.QueryActions>{

    private final QueryMakerGrpc.QueryMakerBlockingStub stub= QueryMakerGrpc.newBlockingStub(channel);


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


        });
    }

    @Override
    protected void runClientCode() throws InterruptedException {
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
        QUERY_CARE("queryCares");

        private final String paramName;
        private Runnable codeToRun;

        QueryActions(String paramName){this.paramName=paramName;}

        @Override
        public String toString(){
            return this.paramName;
        }

    }
}
