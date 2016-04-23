package com.uwc.tooring;

import com.google.gson.Gson;
import com.uwc.tooring.turing.TuringMachine;
import com.uwc.tooring.turing.impl.DefaultTuringMachine;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

/**
 * Unit tests.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TooringApplication.class)
public class TooringApplicationTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(TooringApplicationTests.class);

    private Gson gson = new Gson();

    /**
     * Validates Turing machine serialization to JSON.
     *
     * @throws Exception
     */
    @Test
    public void testSerialization() throws Exception {
        TuringMachine defaultTuringMachine = new DefaultTuringMachine();

        defaultTuringMachine.addState("q1");
        defaultTuringMachine.addState("q2");
        defaultTuringMachine.addState("q3");
        defaultTuringMachine.addState("q4");
        defaultTuringMachine.addState("q5");
        defaultTuringMachine.addState("q6");
        defaultTuringMachine.addState("q7");
        defaultTuringMachine.addState("q8");
        defaultTuringMachine.addState("qa");
        defaultTuringMachine.addState("qr");
        defaultTuringMachine.setStartState("q1");
        defaultTuringMachine.setAcceptState("qa");
        defaultTuringMachine.setRejectState("qr");
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
        String json = "{\"stateSpace\":[\"q1\",\"qa\",\"q2\",\"qr\",\"q3\",\"q4\",\"q5\",\"q6\",\"q7\",\"q8\"],\"transitionSpace\":[{\"readState\":\"q1\",\"readSymbol\":\"1\",\"writeState\":\"q3\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q6\",\"readSymbol\":\"#\",\"writeState\":\"q7\",\"writeSymbol\":\"#\",\"moveDirection\":false},{\"readState\":\"q6\",\"readSymbol\":\"x\",\"writeState\":\"q6\",\"writeSymbol\":\"x\",\"moveDirection\":false},{\"readState\":\"q4\",\"readSymbol\":\"x\",\"writeState\":\"q4\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q4\",\"readSymbol\":\"0\",\"writeState\":\"q6\",\"writeSymbol\":\"x\",\"moveDirection\":false},{\"readState\":\"q5\",\"readSymbol\":\"1\",\"writeState\":\"q6\",\"writeSymbol\":\"x\",\"moveDirection\":false},{\"readState\":\"q2\",\"readSymbol\":\"#\",\"writeState\":\"q4\",\"writeSymbol\":\"#\",\"moveDirection\":true},{\"readState\":\"q5\",\"readSymbol\":\"x\",\"writeState\":\"q5\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q1\",\"readSymbol\":\"0\",\"writeState\":\"q2\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q6\",\"readSymbol\":\"0\",\"writeState\":\"q6\",\"writeSymbol\":\"0\",\"moveDirection\":false},{\"readState\":\"q6\",\"readSymbol\":\"1\",\"writeState\":\"q6\",\"writeSymbol\":\"1\",\"moveDirection\":false},{\"readState\":\"q7\",\"readSymbol\":\"1\",\"writeState\":\"q7\",\"writeSymbol\":\"1\",\"moveDirection\":false},{\"readState\":\"q2\",\"readSymbol\":\"0\",\"writeState\":\"q2\",\"writeSymbol\":\"0\",\"moveDirection\":true},{\"readState\":\"q7\",\"readSymbol\":\"x\",\"writeState\":\"q1\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q3\",\"readSymbol\":\"0\",\"writeState\":\"q3\",\"writeSymbol\":\"0\",\"moveDirection\":true},{\"readState\":\"q8\",\"readSymbol\":\"_\",\"writeState\":\"qa\",\"writeSymbol\":\"_\",\"moveDirection\":true},{\"readState\":\"q3\",\"readSymbol\":\"#\",\"writeState\":\"q5\",\"writeSymbol\":\"#\",\"moveDirection\":true},{\"readState\":\"q8\",\"readSymbol\":\"x\",\"writeState\":\"q8\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q3\",\"readSymbol\":\"1\",\"writeState\":\"q3\",\"writeSymbol\":\"1\",\"moveDirection\":true},{\"readState\":\"q1\",\"readSymbol\":\"#\",\"writeState\":\"q8\",\"writeSymbol\":\"#\",\"moveDirection\":true},{\"readState\":\"q7\",\"readSymbol\":\"0\",\"writeState\":\"q7\",\"writeSymbol\":\"0\",\"moveDirection\":false},{\"readState\":\"q2\",\"readSymbol\":\"1\",\"writeState\":\"q2\",\"writeSymbol\":\"1\",\"moveDirection\":true}],\"startState\":\"q1\",\"acceptState\":\"qa\",\"rejectState\":\"qr\",\"tape\":\"\",\"done\":false}";
        DefaultTuringMachine defaultTuringMachine = gson.fromJson(json, DefaultTuringMachine.class);
        Assert.assertNotNull(defaultTuringMachine);
    }

    /**
     * Validates correct execution of sample algorithm.
     *
     * @throws Exception
     */
    @Test
    public void testTuringMachine() throws Exception {
        String json = "{\"stateSpace\":[\"q1\",\"qa\",\"q2\",\"qr\",\"q3\",\"q4\",\"q5\",\"q6\",\"q7\",\"q8\"],\"transitionSpace\":[{\"readState\":\"q1\",\"readSymbol\":\"1\",\"writeState\":\"q3\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q6\",\"readSymbol\":\"#\",\"writeState\":\"q7\",\"writeSymbol\":\"#\",\"moveDirection\":false},{\"readState\":\"q6\",\"readSymbol\":\"x\",\"writeState\":\"q6\",\"writeSymbol\":\"x\",\"moveDirection\":false},{\"readState\":\"q4\",\"readSymbol\":\"x\",\"writeState\":\"q4\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q4\",\"readSymbol\":\"0\",\"writeState\":\"q6\",\"writeSymbol\":\"x\",\"moveDirection\":false},{\"readState\":\"q5\",\"readSymbol\":\"1\",\"writeState\":\"q6\",\"writeSymbol\":\"x\",\"moveDirection\":false},{\"readState\":\"q2\",\"readSymbol\":\"#\",\"writeState\":\"q4\",\"writeSymbol\":\"#\",\"moveDirection\":true},{\"readState\":\"q5\",\"readSymbol\":\"x\",\"writeState\":\"q5\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q1\",\"readSymbol\":\"0\",\"writeState\":\"q2\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q6\",\"readSymbol\":\"0\",\"writeState\":\"q6\",\"writeSymbol\":\"0\",\"moveDirection\":false},{\"readState\":\"q6\",\"readSymbol\":\"1\",\"writeState\":\"q6\",\"writeSymbol\":\"1\",\"moveDirection\":false},{\"readState\":\"q7\",\"readSymbol\":\"1\",\"writeState\":\"q7\",\"writeSymbol\":\"1\",\"moveDirection\":false},{\"readState\":\"q2\",\"readSymbol\":\"0\",\"writeState\":\"q2\",\"writeSymbol\":\"0\",\"moveDirection\":true},{\"readState\":\"q7\",\"readSymbol\":\"x\",\"writeState\":\"q1\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q3\",\"readSymbol\":\"0\",\"writeState\":\"q3\",\"writeSymbol\":\"0\",\"moveDirection\":true},{\"readState\":\"q8\",\"readSymbol\":\"_\",\"writeState\":\"qa\",\"writeSymbol\":\"_\",\"moveDirection\":true},{\"readState\":\"q3\",\"readSymbol\":\"#\",\"writeState\":\"q5\",\"writeSymbol\":\"#\",\"moveDirection\":true},{\"readState\":\"q8\",\"readSymbol\":\"x\",\"writeState\":\"q8\",\"writeSymbol\":\"x\",\"moveDirection\":true},{\"readState\":\"q3\",\"readSymbol\":\"1\",\"writeState\":\"q3\",\"writeSymbol\":\"1\",\"moveDirection\":true},{\"readState\":\"q1\",\"readSymbol\":\"#\",\"writeState\":\"q8\",\"writeSymbol\":\"#\",\"moveDirection\":true},{\"readState\":\"q7\",\"readSymbol\":\"0\",\"writeState\":\"q7\",\"writeSymbol\":\"0\",\"moveDirection\":false},{\"readState\":\"q2\",\"readSymbol\":\"1\",\"writeState\":\"q2\",\"writeSymbol\":\"1\",\"moveDirection\":true}],\"startState\":\"q1\",\"acceptState\":\"qa\",\"rejectState\":\"qr\",\"tape\":\"010000110101#010000110101\",\"done\":false}";
        DefaultTuringMachine defaultTuringMachine = gson.fromJson(json, DefaultTuringMachine.class);
        defaultTuringMachine.run(null, true);
        Assert.assertTrue(defaultTuringMachine.isDone());
        Assert.assertFalse(StringUtils.isEmpty(defaultTuringMachine.getTape()));
    }

}
