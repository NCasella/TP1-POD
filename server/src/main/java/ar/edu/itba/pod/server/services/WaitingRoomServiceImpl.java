package ar.edu.itba.pod.server.services;

import ar.edu.itba.pod.grpc.Service;
import ar.edu.itba.pod.grpc.WaitingRoomGrpc;
import ar.edu.itba.pod.server.exceptions.FailedDoctorPageException;
import ar.edu.itba.pod.server.exceptions.InternalServerException;
import ar.edu.itba.pod.server.exceptions.PatientNotInWaitingRoomException;
import ar.edu.itba.pod.server.models.Level;
import ar.edu.itba.pod.server.models.Pair;
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
        Pair<Integer,Level> patientsAheadAndLevel = patientRepository.getPatientsAhead(patient);
        responseObserver.onNext(Service.PatientsAhead.newBuilder()
                .setPatients(patientsAheadAndLevel.getFirst())
                .setPatientLevel(Service.Level.forNumber(patientsAheadAndLevel.getSecond().getLevelNumber())).build());
        responseObserver.onCompleted();
    }

    @Override
    public void updatePatientLevel(Service.EnrollmentInfo request, StreamObserver<Service.EnrollmentInfo> responseObserver) {
        Patient patient = patientRepository.getPatientFromName(request.getName());
        try {
            patientRepository.setPatientLevel(patient, Level.getLevelFromNumber(request.getLevelValue()));
        }catch (InterruptedException e){
            throw new InternalServerException();
        }
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }

    @Override
    public void addPatient(Service.EnrollmentInfo request, StreamObserver<Service.EnrollmentInfo> responseObserver) {
        if(request.getLevel().getNumber()<=0||request.getLevel().getNumber()>Level.LEVEL_5.getLevelNumber())
            throw new IllegalArgumentException("Invalid level parameter");
        Level patientLevel=Level.valueOf(request.getLevel().toString());
        try {
            patientRepository.addpatient(request.getName(), patientLevel);
        } catch (InterruptedException e){
            throw new InternalServerException();
        }

        responseObserver.onNext(Service.EnrollmentInfo.newBuilder().setName(request.getName()).setLevel(request.getLevel()).build());
        responseObserver.onCompleted();
    }
}
