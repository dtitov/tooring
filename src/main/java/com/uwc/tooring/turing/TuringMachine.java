package com.uwc.tooring.turing;

/**
 * Interface representing Turing machine.
 */
public interface TuringMachine {

    /**
     * Performs computations over the tape.
     *
     * @param quite true for printing logs
     */
    void run(boolean quite);

    /**
     * Adds state to the machine.
     *
     * @param newState State to add
     * @return true if state was added, false otherwise
     */
    boolean addState(String newState);

    /**
     * Sets start state for the machine.
     *
     * @param newStartState Start state
     * @return true if state was set, false otherwise
     */
    boolean setStartState(String newStartState);

    /**
     * Sets accept state for the machine.
     *
     * @param newAcceptState Accept state
     * @return true if state was set, false otherwise
     */
    boolean setAcceptState(String newAcceptState);

    /**
     * Sets reject state for the machine.
     *
     * @param newRejectState Reject state
     * @return true if state was set, false otherwise
     */
    boolean setRejectState(String newRejectState);

    /**
     * Adds new transition to the machine.
     *
     * @param readState     Triggering state
     * @param readSymbol    Triggering symbol
     * @param writeState    New state
     * @param writeSymbol   New symbol
     * @param moveDirection Direction to move pivot
     * @return true if transition was added, false otherwise
     */
    boolean addTransition(String readState, char readSymbol, String writeState, char writeSymbol, boolean moveDirection);

}
