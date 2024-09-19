package ar.edu.itba.pod.server;

import ar.edu.itba.pod.server.exceptions.*;
import com.google.rpc.Code;
import io.grpc.*;
import io.grpc.protobuf.StatusProto;

import java.util.Map;


public class GlobalExceptionHandlerInterceptor implements ServerInterceptor {

    @Override
    public <T, R> ServerCall.Listener<T> interceptCall(
            ServerCall<T, R> serverCall, Metadata headers, ServerCallHandler<T, R> serverCallHandler) {
        ServerCall.Listener<T> delegate = serverCallHandler.startCall(serverCall, headers);
        return new ExceptionHandler<>(delegate, serverCall, headers);
    }

    private static class ExceptionHandler<T, R> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<T> {

        private final ServerCall<T, R> delegate;
        private final Metadata headers;

        ExceptionHandler(ServerCall.Listener<T> listener, ServerCall<T, R> serverCall, Metadata headers) {
            super(listener);
            this.delegate = serverCall;
            this.headers = headers;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (RuntimeException ex) {
                handleException(ex, delegate, headers);
            }
        }

        private final Map<Class<? extends Throwable>, Code> errorCodesByException = Map.of(
                //todo: ACA VAN LAS EXCEPTIONS
                NoDoctorsAvailableException.class, Code.RESOURCE_EXHAUSTED,
                RoomAlreadyBusyException.class,  Code.RESOURCE_EXHAUSTED,
                RoomIdNotFoundException.class,  Code.NOT_FOUND,
                DoctorNotFoundException.class, Code.NOT_FOUND,
                AppointmentNotFoundException.class, Code.NOT_FOUND,
                IllegalArgumentException.class, Code.INVALID_ARGUMENT,
                DoctorAlreadyRegisteredException.class, Code.ALREADY_EXISTS,
                PatientAlreadyRegisteredException.class, Code.ALREADY_EXISTS,
                DoctorIsAttendingException.class, Code.RESOURCE_EXHAUSTED,
                PatientNotInWaitingRoomException.class, Code.NOT_FOUND
                //DoctorFirstNotificationNotFoundException.class, Code.NOT_FOUND,
                //DoctorAlreadyRegisteredForPagerException.class, Code.ALREADY_EXISTS
        );

        private void handleException(RuntimeException exception, ServerCall<T, R> serverCall, Metadata headers) {
            Throwable error = exception;
            if (!errorCodesByException.containsKey(error.getClass())) {
                // Si la excepción vino "wrappeada" entonces necesitamos preguntar por la causa.
                error = error.getCause();
                if (error == null || !errorCodesByException.containsKey(error.getClass())) {
                    // Una excepción NO esperada.
                    serverCall.close(Status.UNKNOWN, headers);
                    return;
                }
            }
            // Una excepción esperada.
            com.google.rpc.Status rpcStatus = com.google.rpc.Status.newBuilder()
                    .setCode(errorCodesByException.get(error.getClass()).getNumber())
                    .setMessage(error.getMessage())
                    .build();
            StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(rpcStatus);
            Status newStatus = Status.fromThrowable(statusRuntimeException);
            serverCall.close(newStatus, headers);
        }
    }

}