package com.uwc.tooring.turing;

import com.google.gson.Gson;
import com.uwc.tooring.turing.impl.DefaultTuringMachine;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.StringUtils;

import java.util.Random;

/**
 * Unit tests for Turing machine.
 */
public class TuringMachineTests {

    /**
     * Sample Turing machine that tests equality of binary words.
     */
    public static final String TEST_TURING_MACHINE_DESCRIPTION = "{\"stateSpace\":[\"q1\",\"qa\",\"q2\",\"qr\",\"q3\",\"q4\",\"q5\",\"q6\",\"q7\",\"q8\"],\"transitionSpace\":[{\"readState\":\"q1\",\"readSymbol\":\"1\",\"writeState\":\"q3\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q6\",\"readSymbol\":\"#\",\"writeState\":\"q7\",\"writeSymbol\":\"#\",\"moveDirection\":false},{\"readState\":\"q6\",\"readSymbol\":\"x\",\"writeState\":\"q6\",\"writeSymbol\":\"x\",\"moveDirection\":false},{\"readState\":\"q4\",\"readSymbol\":\"x\",\"writeState\":\"q4\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q4\",\"readSymbol\":\"0\",\"writeState\":\"q6\",\"writeSymbol\":\"x\",\"moveDirection\":false},{\"readState\":\"q5\",\"readSymbol\":\"1\",\"writeState\":\"q6\",\"writeSymbol\":\"x\",\"moveDirection\":false},{\"readState\":\"q2\",\"readSymbol\":\"#\",\"writeState\":\"q4\",\"writeSymbol\":\"#\",\"moveDirection\":true},{\"readState\":\"q5\",\"readSymbol\":\"x\",\"writeState\":\"q5\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q1\",\"readSymbol\":\"0\",\"writeState\":\"q2\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q6\",\"readSymbol\":\"0\",\"writeState\":\"q6\",\"writeSymbol\":\"0\",\"moveDirection\":false},{\"readState\":\"q6\",\"readSymbol\":\"1\",\"writeState\":\"q6\",\"writeSymbol\":\"1\",\"moveDirection\":false},{\"readState\":\"q7\",\"readSymbol\":\"1\",\"writeState\":\"q7\",\"writeSymbol\":\"1\",\"moveDirection\":false},{\"readState\":\"q2\",\"readSymbol\":\"0\",\"writeState\":\"q2\",\"writeSymbol\":\"0\",\"moveDirection\":true},{\"readState\":\"q7\",\"readSymbol\":\"x\",\"writeState\":\"q1\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q3\",\"readSymbol\":\"0\",\"writeState\":\"q3\",\"writeSymbol\":\"0\",\"moveDirection\":true},{\"readState\":\"q8\",\"readSymbol\":\"_\",\"writeState\":\"qa\",\"writeSymbol\":\"_\",\"moveDirection\":true},{\"readState\":\"q3\",\"readSymbol\":\"#\",\"writeState\":\"q5\",\"writeSymbol\":\"#\",\"moveDirection\":true},{\"readState\":\"q8\",\"readSymbol\":\"x\",\"writeState\":\"q8\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q3\",\"readSymbol\":\"1\",\"writeState\":\"q3\",\"writeSymbol\":\"1\",\"moveDirection\":true},{\"readState\":\"q1\",\"readSymbol\":\"#\",\"writeState\":\"q8\",\"writeSymbol\":\"#\",\"moveDirection\":true},{\"readState\":\"q7\",\"readSymbol\":\"0\",\"writeState\":\"q7\",\"writeSymbol\":\"0\",\"moveDirection\":false},{\"readState\":\"q2\",\"readSymbol\":\"1\",\"writeState\":\"q2\",\"writeSymbol\":\"1\",\"moveDirection\":true}],\"startState\":\"q1\",\"acceptState\":\"qa\",\"tape\":\"010000110101#010000110101\",\"done\":false}";

    /**
     * Defines size of the array to sort.
     * Be careful: with value 100 it takes ~30 minutes to sort the array.
     */
    public static final int MULTIPLIER = 1;

    private Gson gson = new Gson();

    /**
     * Validates Turing machine serialization to JSON.
     *
     * @throws Exception
     */
    @Test
    public void testSerialization() throws Exception {
        TuringMachine defaultTuringMachine = new DefaultTuringMachine();

        defaultTuringMachine.setStartState("q1");
        defaultTuringMachine.setAcceptState("qa");

        defaultTuringMachine.addTransition("q1", '1', "q3", 'x', true);
        defaultTuringMachine.addTransition("q1", '0', "q2", 'x', true);
        defaultTuringMachine.addTransition("q1", '#', "q8", '#', true);
        defaultTuringMachine.addTransition("q2", '0', "q2", '0', true);
        defaultTuringMachine.addTransition("q2", '1', "q2", '1', true);
        defaultTuringMachine.addTransition("q2", '#', "q4", '#', true);
        defaultTuringMachine.addTransition("q3", '0', "q3", '0', true);
        defaultTuringMachine.addTransition("q3", '1', "q3", '1', true);
        defaultTuringMachine.addTransition("q3", '#', "q5", '#', true);
        defaultTuringMachine.addTransition("q4", 'x', "q4", 'x', true);
        defaultTuringMachine.addTransition("q4", '0', "q6", 'x', false);
        defaultTuringMachine.addTransition("q5", 'x', "q5", 'x', true);
        defaultTuringMachine.addTransition("q5", '1', "q6", 'x', false);
        defaultTuringMachine.addTransition("q6", '0', "q6", '0', false);
        defaultTuringMachine.addTransition("q6", '1', "q6", '1', false);
        defaultTuringMachine.addTransition("q6", 'x', "q6", 'x', false);
        defaultTuringMachine.addTransition("q6", '#', "q7", '#', false);
        defaultTuringMachine.addTransition("q7", '0', "q7", '0', false);
        defaultTuringMachine.addTransition("q7", '1', "q7", '1', false);
        defaultTuringMachine.addTransition("q7", 'x', "q1", 'x', true);
        defaultTuringMachine.addTransition("q8", 'x', "q8", 'x', true);
        defaultTuringMachine.addTransition("q8", '_', "qa", '_', true);

        String json = gson.toJson(defaultTuringMachine);
        Assert.assertFalse(StringUtils.isEmpty(json));
    }

    /**
     * Validates Turing machine deserialization from JSON.
     *
     * @throws Exception
     */
    @Test
    public void testDeserialization() throws Exception {
        DefaultTuringMachine defaultTuringMachine = gson.fromJson(TEST_TURING_MACHINE_DESCRIPTION, DefaultTuringMachine.class);
        Assert.assertNotNull(defaultTuringMachine);
    }

    /**
     * Validates correct execution of sample algorithm.
     *
     * @throws Exception
     */
    @Test
    public void testTuringMachineOnSampleData() throws Exception {
        DefaultTuringMachine defaultTuringMachine = gson.fromJson(TEST_TURING_MACHINE_DESCRIPTION, DefaultTuringMachine.class);
        defaultTuringMachine.run(true);
        Assert.assertTrue(defaultTuringMachine.isDone());
        Assert.assertFalse(StringUtils.isEmpty(defaultTuringMachine.getTape()));
    }

    @Test
    public void testBubbleSort() throws Exception {
        DefaultTuringMachine bubbleSort = new DefaultTuringMachine();

        // R: "Run" (normal)
        bubbleSort.addTransition("R", 'a', "R", null, true);
        bubbleSort.addTransition("R", 'b', "B", null, true);
        bubbleSort.addTransition("R", 'c', "C", null, true);

        // B: "the last character read was b"
        bubbleSort.addTransition("B", 'c', "C", null, true);
        bubbleSort.addTransition("B", 'b', "B", null, true);

        bubbleSort.addTransition("B", 'a', "Ba", 'b', null);
        bubbleSort.addTransition("Ba", 'b', "Wa", null, false);

        // C: "the last character read was c"
        bubbleSort.addTransition("C", 'c', "C", null, true);

        bubbleSort.addTransition("C", 'b', "Cb", 'c', null);
        bubbleSort.addTransition("Cb", 'c', "Wb", null, false);

        bubbleSort.addTransition("C", 'a', "Ca", 'c', null);
        bubbleSort.addTransition("Ca", 'c', "Wa", null, false);

        // Wx: "Write x"
        bubbleSort.addTransition("Wa", 'b', "R", 'a', null);
        bubbleSort.addTransition("Wa", 'c', "R", 'a', null);
        bubbleSort.addTransition("Wb", 'c', "R", 'b', null);

        // ML: "Mark last"
        bubbleSort.addTransition("R", DefaultTuringMachine.EMPTY, "ML", null, false);
        bubbleSort.addTransition("R", 'A', "ML", null, false);
        bubbleSort.addTransition("R", 'B', "ML", null, false);
        bubbleSort.addTransition("R", 'C', "ML", null, false);
        bubbleSort.addTransition("B", DefaultTuringMachine.EMPTY, "ML", null, false);
        bubbleSort.addTransition("B", 'B', "ML", null, false);
        bubbleSort.addTransition("B", 'C', "ML", null, false);
        bubbleSort.addTransition("C", DefaultTuringMachine.EMPTY, "ML", null, false);
        bubbleSort.addTransition("C", 'C', "ML", null, false);

        // RTS: "Return to start"
        bubbleSort.addTransition("ML", 'a', "RTS", 'A', null);
        bubbleSort.addTransition("ML", 'b', "RTS", 'B', null);
        bubbleSort.addTransition("ML", 'c', "RTS", 'C', null);

        bubbleSort.addTransition("RTS", 'a', "RTS", null, false);
        bubbleSort.addTransition("RTS", 'A', "RTS", null, false);
        bubbleSort.addTransition("RTS", 'b', "RTS", null, false);
        bubbleSort.addTransition("RTS", 'B', "RTS", null, false);
        bubbleSort.addTransition("RTS", 'c', "RTS", null, false);
        bubbleSort.addTransition("RTS", 'C', "RTS", null, false);

        bubbleSort.addTransition("RTS", DefaultTuringMachine.EMPTY, "R", null, true);

        // CL: "Clean up"
        bubbleSort.addTransition("ML", DefaultTuringMachine.EMPTY, "CL", null, true);
        bubbleSort.addTransition("CL", 'A', "CL", 'a', null);
        bubbleSort.addTransition("CL", 'a', "CL", null, true);
        bubbleSort.addTransition("CL", 'B', "CL", 'b', null);
        bubbleSort.addTransition("CL", 'b', "CL", null, true);
        bubbleSort.addTransition("CL", 'C', "CL", 'c', null);
        bubbleSort.addTransition("CL", 'c', "CL", null, true);

        bubbleSort.addTransition("CL", DefaultTuringMachine.EMPTY, "F", null, true);

        // F: "Finished"
        bubbleSort.addTransition("R", 'A', "F", 'a', null);
        bubbleSort.addTransition("R", 'B', "F", 'b', null);
        bubbleSort.addTransition("R", 'C', "F", 'c', null);

        bubbleSort.setStartState("R");
        bubbleSort.setAcceptState("F");

        Random random = new Random();
        String tape = "";
        for (int i = 0; i < Byte.MAX_VALUE * MULTIPLIER; i++) {
            tape += random.nextBoolean() ? "a" : "b";
        }
        bubbleSort.setTape(tape);
        bubbleSort.run(true);
        Assert.assertTrue(bubbleSort.isDone());
        Assert.assertFalse(StringUtils.isEmpty(bubbleSort.getTape()));
    }

}
