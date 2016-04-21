package com.uwc.tooring.model;

import java.io.Serializable;

/**
 * Class represents transition of Turing machine.
 */
public class Transition implements Serializable {

    private final String readState;
    private final char readSymbol;
    private final String writeState;
    private final char writeSymbol;
    private final boolean moveDirection;

    /**
     * Constructor accepting all required parameters for building the transition.
     *
     * @param readState     Triggering state
     * @param readSymbol    Triggering symbol
     * @param writeState    New state
     * @param writeSymbol   New symbol
     * @param moveDirection Direction to move pivot
     */
    public Transition(String readState, char readSymbol, String writeState, char writeSymbol, boolean moveDirection) {
        this.readState = readState;
        this.readSymbol = readSymbol;
        this.writeState = writeState;
        this.writeSymbol = writeSymbol;
        this.moveDirection = moveDirection;
    }

    /**
     * Checks for conflict condition: state equals read state and symbol equals read symbol.
     *
     * @param state  Input state
     * @param symbol Input symbol
     * @return true if it's conflicting condition, false otherwise
     */
    public boolean isConflicting(String state, char symbol) {
        return state.equals(readState) && symbol == readSymbol;
    }

    /**
     * Returns read state.
     *
     * @return Read state
     */
    public String getReadState() {
        return readState;
    }

    /**
     * Returns read symbol.
     *
     * @return Read symbol
     */
    public char getReadSymbol() {
        return readSymbol;
    }

    /**
     * Returns write state.
     *
     * @return Write state
     */
    public String getWriteState() {
        return writeState;
    }

    /**
     * Returns write symbol.
     *
     * @return Write symbol
     */
    public char getWriteSymbol() {
        return writeSymbol;
    }

    /**
     * Returns move direction: true stands for "right", false stands for "left".
     *
     * @return Movement direction
     */
    public boolean isMoveDirection() {
        return moveDirection;
    }

    /**
     * {@inheritDoc}
     */
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
