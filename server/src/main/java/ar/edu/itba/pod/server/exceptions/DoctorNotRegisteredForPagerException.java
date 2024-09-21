package ar.edu.itba.pod.server.exceptions;

public class DoctorNotRegisteredForPagerException extends RuntimeException {
    private final static String MESSAGE = "Doctor %s is not registered for pager";
    public DoctorNotRegisteredForPagerException(String doctorName) {
        super(MESSAGE.formatted(doctorName));
    }
}


