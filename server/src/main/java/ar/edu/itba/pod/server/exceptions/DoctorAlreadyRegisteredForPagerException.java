package ar.edu.itba.pod.server.exceptions;

public class DoctorAlreadyRegisteredForPagerException extends RuntimeException {
    private final static String MESSAGE = "Doctor %s already registered for pager";
    public DoctorAlreadyRegisteredForPagerException(String doctorName) {
        super(MESSAGE.formatted(doctorName));
    }
}
