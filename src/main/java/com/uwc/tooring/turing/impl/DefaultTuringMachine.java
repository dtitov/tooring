package com.uwc.tooring.turing.impl;

import com.uwc.tooring.model.Transition;
import com.uwc.tooring.turing.TuringMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Default implementation of Turing machine.
 */
public class DefaultTuringMachine implements TuringMachine, Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTuringMachine.class);

    private Set<String> stateSpace = new HashSet<>();
    private Set<Transition> transitionSpace = new HashSet<>();
    private String startState = "";
    private String acceptState = "";
    private String rejectState = "";
    private String tape = "";
    private boolean done = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(boolean quite) {
        String currentState = startState;
        int currentSymbol = 0;

        while (!currentState.equals(acceptState) && !currentState.equals(rejectState)) {
            boolean foundTransition = false;
            Transition currentTransition = null;

            if (!quite) {
                if (currentSymbol > 0) {
                    LOGGER.info(tape.substring(0, currentSymbol) + " " + currentState + " " + tape.substring(currentSymbol));
                } else {
                    LOGGER.info(" " + currentState + " " + tape.substring(currentSymbol));
                }
            }

            Iterator<Transition> transitionsIterator = transitionSpace.iterator();
            while (transitionsIterator.hasNext() && !foundTransition) {
                Transition nextTransition = transitionsIterator.next();
                if (nextTransition.getReadState().equals(currentState) && nextTransition.getReadSymbol() == tape.charAt(currentSymbol)) {
                    foundTransition = true;
                    currentTransition = nextTransition;
                }
            }

            if (!foundTransition) {
                LOGGER.error("There is no valid transition for this phase! (state=" + currentState + ", symbol=" + tape.charAt(currentSymbol) + ")");
                return;
            } else {
                currentState = currentTransition.getWriteState();
                char[] tempTape = tape.toCharArray();
                tempTape[currentSymbol] = currentTransition.getWriteSymbol();
                tape = new String(tempTape);
                if (currentTransition.isMoveDirection()) {
                    currentSymbol++;
                } else {
                    currentSymbol--;
                }

                if (currentSymbol < 0) {
                    currentSymbol = 0;
                }

                while (tape.length() <= currentSymbol) {
                    tape = tape.concat("_");
                }
            }
        }

        done = currentState.equals(acceptState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addState(String newState) {
        if (stateSpace.contains(newState)) {
            return false;
        } else {
            stateSpace.add(newState);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setStartState(String newStartState) {
        if (stateSpace.contains(newStartState)) {
            startState = newStartState;
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setAcceptState(String newAcceptState) {
        if (stateSpace.contains(newAcceptState) && !rejectState.equals(newAcceptState)) {
            acceptState = newAcceptState;
            return true;
        } else {
            return false;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setRejectState(String newRejectState) {
        if (stateSpace.contains(newRejectState) && !acceptState.equals(newRejectState)) {
            rejectState = newRejectState;
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addTransition(String readState, char readSymbol, String writeState, char writeSymbol, boolean moveDirection) {
        if (!stateSpace.contains(readState) || !stateSpace.contains(writeState)) {
            return false;
        }

        boolean conflict = false;
        Iterator<Transition> transitionsIterator = transitionSpace.iterator();
        while (transitionsIterator.hasNext() && !conflict) {
            Transition nextTransition = transitionsIterator.next();
            if (nextTransition.isConflicting(readState, readSymbol)) {
                conflict = true;
            }
        }
        if (conflict) {
            return false;
        } else {
            Transition newTransition = new Transition(readState, readSymbol, writeState, writeSymbol, moveDirection);
            transitionSpace.add(newTransition);
            return true;
        }
    }

    /**
     * Gets state space of the machine.
     *
     * @return State space
     */
    public Set<String> getStateSpace() {
        return stateSpace;
    }

    /**
     * Sets state space of the machine.
     *
     * @param stateSpace
     */
    public void setStateSpace(Set<String> stateSpace) {
        this.stateSpace = stateSpace;
    }

    /**
     * Gets transition space of the machine.
     *
     * @return Transition space
     */
    public Set<Transition> getTransitionSpace() {
        return transitionSpace;
    }

    /**
     * Sets transition space of the machine.
     *
     * @param transitionSpace Transition space
     */
    public void setTransitionSpace(Set<Transition> transitionSpace) {
        this.transitionSpace = transitionSpace;
    }

    /**
     * Gets start state.
     *
     * @return Start state.
     */
    public String getStartState() {
        return startState;
    }

    /**
     * Gets accept state.
     *
     * @return Accept state
     */
    public String getAcceptState() {
        return acceptState;
    }

    /**
     * Gets reject state.
     *
     * @return Reject state
     */
    public String getRejectState() {
        return rejectState;
    }

    /**
     * Gets machine tape.
     *
     * @return Machine tape
     */
    public String getTape() {
        return tape;
    }

    /**
     * Sets tape to the machine.
     *
     * @param tape Machine tape
     */
    public void setTape(String tape) {
        this.tape = tape;
    }

    /**
     * Checks if computations are finished.
     *
     * @return true if tape computation is completed, false otherwise
     */
    public boolean isDone() {
        return done;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DefaultTuringMachine{" +
                "stateSpace=" + stateSpace +
                ", transitionSpace=" + transitionSpace +
                ", startState='" + startState + '\'' +
                ", acceptState='" + acceptState + '\'' +
                ", rejectState='" + rejectState + '\'' +
                ", tape='" + tape + '\'' +
                ", done=" + done +
                '}';
    }

}