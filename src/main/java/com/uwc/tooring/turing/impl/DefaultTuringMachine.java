package com.uwc.tooring.turing.impl;

import com.uwc.tooring.model.Transition;
import com.uwc.tooring.turing.TuringMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Default implementation of Turing machine.
 */
public class DefaultTuringMachine implements TuringMachine, Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTuringMachine.class);

    public static Character EMPTY = '_';

    private String id;
    private boolean scheduled;
    private boolean locked;
    private boolean done;

    private Set<String> stateSpace = new HashSet<>();
    private Set<Transition> transitionSpace = new HashSet<>();

    private String startState;
    private String acceptState;

    private String tape;

    private String currentState;
    private Integer currentIndex;

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(boolean quite) {
        this.locked = true;
        try {
            // Init current state and symbol in case of new computation or use last values otherwise
            if (StringUtils.isEmpty(currentState) && currentIndex == null) {
                currentState = startState;
                currentIndex = 0;
            }

            while (!currentState.equals(acceptState)) {
                boolean foundTransition = false;
                Transition currentTransition = null;

                if (!quite) {
                    if (currentIndex > 0) {
                        LOGGER.info(tape.substring(0, currentIndex) + " " + currentState + " " + tape.substring(currentIndex));
                    } else {
                        LOGGER.info(" " + currentState + " " + tape.substring(currentIndex));
                    }
                }

                Iterator<Transition> transitionsIterator = transitionSpace.iterator();
                while (transitionsIterator.hasNext() && !foundTransition) {
                    Transition nextTransition = transitionsIterator.next();
                    if (nextTransition.getReadState().equals(currentState) && nextTransition.getReadSymbol() == tape.charAt(currentIndex)) {
                        foundTransition = true;
                        currentTransition = nextTransition;
                    }
                }

                if (!foundTransition) {
                    throw new IllegalStateException("There is no valid transition for this phase! (state=" + currentState + ", symbol=" + tape.charAt(currentIndex) + ")");
                }

                currentState = currentTransition.getWriteState();
                char[] tempTape = tape.toCharArray();
                Character writeSymbol = currentTransition.getWriteSymbol();
                if (writeSymbol != null) {
                    tempTape[currentIndex] = writeSymbol;
                }
                tape = new String(tempTape);
                if (currentTransition.isMoveDirection() != null) {
                    if (currentTransition.isMoveDirection()) {
                        currentIndex++;
                    } else {
                        currentIndex--;
                    }
                }

                if (currentIndex < 0) {
                    tape = EMPTY + tape;
                    currentIndex = 0;
                }

                while (tape.length() <= currentIndex) {
                    tape = tape.concat("_");
                }
            }
            cleanUpTape();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            scheduled = false;
            locked = false;
            done = true;
        }
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
        if (stateSpace.contains(newAcceptState)) {
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
    public boolean addTransition(String readState, Character readSymbol, String writeState, Character writeSymbol, Boolean moveDirection) {
        if (!stateSpace.contains(readState)) {
            if (!addState(readState)) {
                throw new IllegalArgumentException("Can't add transition, because read state can't be added to the state space");
            }
        }
        if (!stateSpace.contains(writeState)) {
            if (!addState(writeState)) {
                throw new IllegalArgumentException("Can't add transition, because write state can't be added to the state space");
            }
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
     * Clean opening and ending whitespaces in tape (empty symbols).
     */
    private void cleanUpTape() {
        tape = tape.replace('_', ' ').trim();
    }

    /**
     * Gets ID of submitter.
     *
     * @return ID of submitter
     */
    public String getId() {
        return id;
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
     * @param stateSpace State space
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
     * Schedules machine for execution.
     */
    public void schedule(String id) {
        this.id = id;
        this.scheduled = true;
    }

    /**
     * Checks if machine is scheduled for execution.
     *
     * @return true if machine is scheduled for execution, false otherwise
     */
    public boolean isScheduled() {
        return scheduled;
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
     * Checks if current machine is locked.
     *
     * @return true if current machine is locked, false otherwise
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Sets locking state of current machine.
     *
     * @param locked lock-flag
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
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
                ", tape='" + tape + '\'' +
                ", scheduled=" + scheduled +
                ", locked=" + locked +
                ", done=" + done +
                '}';
    }

}