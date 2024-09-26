package ar.edu.itba.pod.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.j2objc.annotations.Property;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public abstract class Client<T extends Enum<T> > {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    protected T actionProperty;
    protected ManagedChannel channel;
    protected Map<T,Runnable> actionMapper;
    protected final CountDownLatch countDownLatch= new CountDownLatch(1);

    protected <K> FutureCallback<K> getPrintStreamObserver(Consumer<K> consumer){
        return new FutureCallback<>() {
            @Override
            public void onSuccess(K k) {
                consumer.accept(k);
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(Throwable throwable) {
                System.out.println(throwable.getMessage());
                countDownLatch.countDown();
            }
        };
    }
    /*
    * metodo que prepara la conexion con el servidor. Este es el metodo que se debe llamar
    */
    protected final void startClient() throws InterruptedException {
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
        int port;
        try {
            port=Integer.parseInt(host[1]);
        }catch (NumberFormatException e){
            System.out.println("Error parsing port number");
            return;
        }
        channel = ManagedChannelBuilder.forAddress(host[0], port)
                .usePlaintext()
                .build();
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
