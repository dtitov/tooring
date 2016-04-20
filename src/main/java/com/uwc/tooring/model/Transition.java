package com.uwc.tooring.model;

public class Transition {
    private final String readState;
    private final char readSymbol;
    private final String writeState;
    private final char writeSymbol;
    private final boolean moveDirection;    //true is right, false is left

    public Transition(String readState, char readSymbol, String writeState, char writeSymbol, boolean moveDirection) {
        this.readState = readState;
        this.readSymbol = readSymbol;
        this.writeState = writeState;
        this.writeSymbol = writeSymbol;
        this.moveDirection = moveDirection;
    }

    public boolean isConflicting(String state, char symbol) {
        return state.equals(readState) && symbol == readSymbol;
    }

    public String getReadState() {
        return readState;
    }

    public char getReadSymbol() {
        return readSymbol;
    }

    public String getWriteState() {
        return writeState;
    }

    public char getWriteSymbol() {
        return writeSymbol;
    }

    public boolean isMoveDirection() {
        return moveDirection;
    }

    @Override
    public String toString() {
        return "Transition{" +
                "readState='" + readState + '\'' +
                ", readSymbol=" + readSymbol +
                ", writeState='" + writeState + '\'' +
                ", writeSymbol=" + writeSymbol +
                ", moveDirection=" + moveDirection +
                '}';
    }

}
