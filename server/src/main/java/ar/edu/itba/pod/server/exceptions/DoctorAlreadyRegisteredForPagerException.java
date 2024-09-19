package ar.edu.itba.pod.server.exceptions;

public class DoctorAlreadyRegisteredForPagerException extends RuntimeException {
    public DoctorAlreadyRegisteredForPagerException(String message) {
        super(message);
    }
}
