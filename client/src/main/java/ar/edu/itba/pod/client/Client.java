package ar.edu.itba.pod.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public abstract class Client<T extends Enum<T> > {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    protected T actionProperty;
    protected ManagedChannel channel;
    Map<T,Runnable> actionMapper;
    CountDownLatch countDownLatch= new CountDownLatch(1);

    /*
    * metodo que prepara la conexion con el servidor. Este es el metodo que se debe llamar
    */
    protected final void startClient() throws InterruptedException {
        logger.info("Client Starting ...");
        logger.info("grpc-com-patterns Client Starting ...");
        String[] host = System.getProperty("serverAddress" ,"localhost:50051").split(":");
        if(host.length!=2){
            System.out.println("invalid address:port combination");
            return;
        }
        String actionParam=System.getProperty("action");

        if(actionParam==null){
            System.out.println("action parameter not specified");
            return;
        }

        channel = ManagedChannelBuilder.forAddress(host[0], Integer.parseInt(host[1]))
                .usePlaintext()
                .build();
        logger.info("created channel at host:{} port:{}",host[0],host[1]);
        Optional<T> optional= Arrays.stream(getEnumClass().getEnumConstants()).
                filter((arrayVal)->arrayVal.toString().equals(actionParam)).findFirst();

        if(optional.isEmpty()){
            System.out.println("invalid action parameter");
            return;
        }
        actionProperty=optional.get();
        try {
            runClientCode();
        } finally {

            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    //debe devolver la clase del enum de las acciones posibles del servicio
    protected abstract Class<T> getEnumClass();

    //implementa el codigo correspondiente a cada cliente
    protected abstract void runClientCode() throws InterruptedException;
}
