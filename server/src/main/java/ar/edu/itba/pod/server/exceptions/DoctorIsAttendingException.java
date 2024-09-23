package ar.edu.itba.pod.server.exceptions;

public class DoctorIsAttendingException extends RuntimeException{
    private final static String MESSAGE = "Doctor %s is currently attending a patient";
    public DoctorIsAttendingException(String doctorName) {
        super(MESSAGE.formatted(doctorName));
    }
}
