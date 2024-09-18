package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.grpc.WaitingRoomGrpc;
import ar.edu.itba.pod.server.models.Level;
import ar.edu.itba.pod.server.models.Patient;
import ar.edu.itba.pod.server.repositories.PatientRepository;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;

public class WaitingRoomServiceImpl extends WaitingRoomGrpc.WaitingRoomImplBase {
    private final PatientRepository patientRepository;

    public WaitingRoomServiceImpl(PatientRepository patientRepository){
        this.patientRepository=patientRepository;
    }
    @Override
    public void getPatientsAhead(StringValue request, StreamObserver<Service.PatientsAhead> responseObserver) {
        Patient patient= patientRepository.getPatientFromName(request.getValue());
        int patientsAhead= patientRepository.getPatientsAhead(patient);
        responseObserver.onNext(Service.PatientsAhead.newBuilder().setPatients(patientsAhead)
                .setPatientLevel(Service.Level.forNumber(patient.getPatientLevel().getLevelNumber())).build());
        responseObserver.onCompleted();
    }

    @Override
    public void updatePatientLevel(Service.EnrollmentInfo request, StreamObserver<Service.EnrollmentInfo> responseObserver) {
        super.updatePatientLevel(request, responseObserver);
    }

    @Override
    public void addPatient(Service.EnrollmentInfo request, StreamObserver<Service.EnrollmentInfo> responseObserver) {
        if(request.getLevel().getNumber()<=0||request.getLevel().getNumber()>5)
            throw new IllegalArgumentException("Invalid level parameter");
        Level patientLevel=Level.valueOf(request.getLevel().toString());
        patientRepository.addpatient(request.getName(),patientLevel);

        responseObserver.onNext(Service.EnrollmentInfo.newBuilder().setName(request.getName()).setLevel(request.getLevel()).build());
        responseObserver.onCompleted();
    }
}
