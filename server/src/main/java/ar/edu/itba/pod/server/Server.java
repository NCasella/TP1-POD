package ar.edu.itba.pod.server;

import ar.edu.itba.pod.server.repositories.*;
import ar.edu.itba.pod.server.services.*;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Server {
    private static final DoctorRepository doctorRepository= new DoctorRepository();
    private static final PatientRepository patientRepository=new PatientRepository();
    private static final RoomRepository roomRepository=new RoomRepository();
    private static final NotificationRepository notificationRepository = new NotificationRepository(doctorRepository);
    private static final FinishedAppointmentRepository finishedAppointmentRepository = new FinishedAppointmentRepository();
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        logger.info(" Server Starting ...");
        int port = 50051;


        io.grpc.Server server = ServerBuilder.forPort(port).intercept(new GlobalExceptionHandlerInterceptor())
                .addService(new EmergencyAttentionServiceImpl(patientRepository, doctorRepository, roomRepository, notificationRepository, finishedAppointmentRepository))
                .addService(new AdminService(doctorRepository,roomRepository,notificationRepository))
                .addService(new DoctorPagerService(doctorRepository,notificationRepository))
                .addService(new WaitingRoomServiceImpl(patientRepository))
                .addService(new QueryServiceImpl(patientRepository,roomRepository,finishedAppointmentRepository))
                .build();
        server.start();

        logger.info("Server started, listening on " + port);
        server.awaitTermination();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server since JVM is shutting down");
            server.shutdown();
            logger.info("Server shut down");
        }));
    }}
