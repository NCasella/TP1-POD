package ar.edu.itba.pod.server.exceptions;

public class DoctorFirstNotificationNotFoundException extends RuntimeException {
    private final static String MESSAGE = "Doctor %s First Notification Not Found";
    public DoctorFirstNotificationNotFoundException(String doctorName) {
        super(MESSAGE.formatted(doctorName));
    }
}
