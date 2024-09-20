package ar.edu.itba.pod.server.models;

import ar.edu.itba.pod.grpc.Service;

import java.util.Arrays;

public enum Level {
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3),
    LEVEL_4(4),
    LEVEL_5(5);

    private final int levelNumber;
    private final static Level[] values = Level.values();

    Level(int levelNumber){
        this.levelNumber=levelNumber;
    }

    public static Level getLevelFromNumber(int levelNumber){
        return Arrays.stream(Level.values()).filter((level -> level.levelNumber==levelNumber))
                .findFirst().orElseThrow(IllegalArgumentException::new);
    }
    public  int getLevelNumber(){return this.levelNumber;}

    public Service.Level toGrpc(){
         return Service.Level.forNumber(levelNumber);
    }

    public static Level of(Service.Level level){
        return values[level.getNumber()];
    }
}
