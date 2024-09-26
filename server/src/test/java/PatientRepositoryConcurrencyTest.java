import ar.edu.itba.pod.server.models.Level;
import ar.edu.itba.pod.server.models.Pair;
import ar.edu.itba.pod.server.models.Patient;
import ar.edu.itba.pod.server.repositories.PatientRepository;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PatientRepositoryConcurrencyTest {

    private final PatientRepository patientRepository = new PatientRepository();
    private static final int BASICNUMTHREADS= 1000;

    @Test
    public void testAddingPatientToQueue() {
        ExecutorService executor = Executors.newFixedThreadPool(BASICNUMTHREADS);
        List<Callable<Boolean>> operations = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(BASICNUMTHREADS);
        for (int currentNum = 0; currentNum < BASICNUMTHREADS; currentNum++) {
            int finalCurrentNum = currentNum;
            operations.add(() -> {
                try {
                    String currentPatient = "patient" + finalCurrentNum;
                    patientRepository.addpatient(currentPatient, Level.LEVEL_1);
                    return true;
                }catch (InterruptedException e){
                   return false;
                }
                finally {
                    countDownLatch.countDown();
                }
            });
        }
        try {
            List<Future<Boolean>> result =  executor.invokeAll(operations);
            countDownLatch.await();
            for (Future<Boolean> future : result){
                Boolean finalValue = future.get();
                if (!finalValue){
                    System.out.println("Something went wrong during a single thread execution...");
                }
            }

        }catch (InterruptedException e){
            System.out.println("Interrupted Exception was caught, something went wrong...");
        } catch (ExecutionException e) {
            System.out.println("Execution Exception was caught, something went wrong during computation...");
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPatientsAhead(){
        ExecutorService executor = Executors.newFixedThreadPool(BASICNUMTHREADS);
        List<Callable<Boolean>> operations = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(BASICNUMTHREADS);
        testAddingPatientToQueue();
        List<Patient> patientsList = patientRepository.getWaitingRoomList();
        for (int currentNum = 0; currentNum < BASICNUMTHREADS; currentNum++) {
            int finalCurrentNum = currentNum;
            Callable<Boolean> getPatientsAhead = () -> {
                try {
                    Pair<Integer, Level> result = patientRepository.getPatientsAhead(patientsList.get(finalCurrentNum));
                    Assertions.assertEquals(finalCurrentNum, result.getFirst());
                    return true;
                } catch (RuntimeException e){
                    return false;
                }
                finally {
                    countDownLatch.countDown();
                }
            };
            operations.add(getPatientsAhead);
        }
        try {
            List<Future<Boolean>> result =  executor.invokeAll(operations);
            countDownLatch.await();
            for (Future<Boolean> future : result){
                Boolean finalValue = future.get();
                if (!finalValue){
                    System.out.println("Something went wrong during a single thread execution...");
                }
            }

        }catch (InterruptedException e){
            System.out.println("Interrupted Exception was caught, something went wrong...");
        } catch (ExecutionException e) {
            System.out.println("Execution Exception was caught, something went wrong during computation...");
            throw new RuntimeException(e);
        }

    }

    @Test
    public void testSetPatientLevel(){
        ExecutorService executor = Executors.newFixedThreadPool(BASICNUMTHREADS);
        List<Callable<Boolean>> operations = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(BASICNUMTHREADS);
        testAddingPatientToQueue();
        List<Patient> patientsList = patientRepository.getWaitingRoomList();
        for (int currentNum = 0; currentNum < BASICNUMTHREADS; currentNum++) {
            int finalCurrentNum = currentNum;
            Callable<Boolean> setPatientLevelCallable= () -> {
                try {
                    patientRepository.setPatientLevel(patientsList.get(finalCurrentNum), Level.LEVEL_1);
                    Patient patient = patientRepository.getPatientFromName(patientsList.get(finalCurrentNum).getPatientName());
                    Assertions.assertEquals(Level.LEVEL_1, patient.getPatientLevel());
                    return true;
                } catch (RuntimeException e){
                    return false;
                }
                finally {
                    countDownLatch.countDown();
                }
            };
            operations.add(setPatientLevelCallable);
        }
        try {
            List<Future<Boolean>> result =  executor.invokeAll(operations);
            countDownLatch.await();
            for (Future<Boolean> future : result){
                Boolean finalValue = future.get();
                if (!finalValue){
                    System.out.println("Something went wrong during a single thread execution...");
                }
            }

        }catch (InterruptedException e){
            System.out.println("Interrupted Exception was caught, something went wrong...");
        } catch (ExecutionException e) {
            System.out.println("Execution Exception was caught, something went wrong during computation...");
            throw new RuntimeException(e);
        }

    }

}
