package ar.edu.itba.pod.server.exceptions;

public class DoctorNotFoundException extends RuntimeException{
    private static final String MESSAGE = "There is no Doctor with that name.";

    //se podría pasar el nombre para que sea más custom
    @Override
    public String toString(){
        return MESSAGE;
    }

}
