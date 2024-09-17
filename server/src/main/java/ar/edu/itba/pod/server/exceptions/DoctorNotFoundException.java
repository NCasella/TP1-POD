package ar.edu.itba.pod.server.exceptions;

public class DoctorNotFoundException extends RuntimeException{
    private static final String MESSAGE = "Doctor %s is not registered.";
    private final String name;

    public DoctorNotFoundException(String name){
        this.name = name;
    }

    //se podría pasar el nombre para que sea más custom
    @Override
    public String toString(){
        return String.format(MESSAGE,name);
    }

}
