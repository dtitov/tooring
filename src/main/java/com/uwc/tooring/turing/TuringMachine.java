package com.uwc.tooring.turing;

public interface TuringMachine {

    void run(boolean quite);

    boolean addState(String newState);

    boolean setStartState(String newStartState);

    boolean setAcceptState(String newAcceptState);

    boolean setRejectState(String newRejectState);

    boolean addTransition(String readState, char readSymbol, String writeState, char writeSymbol, boolean moveDirection);

}
