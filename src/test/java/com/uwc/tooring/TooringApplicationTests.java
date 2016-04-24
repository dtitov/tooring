package com.uwc.tooring;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.uwc.tooring.turing.TuringMachineTests;
import com.uwc.tooring.turing.impl.DefaultTuringMachine;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for the application.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TooringApplication.class)
public class TooringApplicationTests {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private TuringService turingService;

    private Gson gson = new Gson();

    /**
     * Common scenario: submits task (JSON-formatted Turing machine), schedules it for execution, starts as worker for specified time, gets the result of computation.
     * Asserts control flags of the Turing machine after each step.
     */
    @Test
    public void testTuringService() {
        String key = turingService.processInputJSON(TuringMachineTests.TEST_TURING_MACHINE_DESCRIPTION);
        IMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getMap(TuringService.TASKS_MAP);
        Assert.assertFalse(CollectionUtils.isEmpty(tasksMap.entrySet()));
        Map.Entry<String, DefaultTuringMachine> turingMachineEntry = tasksMap.entrySet().iterator().next();
        Assert.assertEquals(key, turingMachineEntry.getKey());
        DefaultTuringMachine turingMachine = turingMachineEntry.getValue();
        Assert.assertNotNull(turingMachine);
        Assert.assertFalse(turingMachine.isScheduled());
        Assert.assertFalse(turingMachine.isLocked());
        Assert.assertFalse(turingMachine.isDone());

        turingService.scheduleExecution(TooringApplicationTests.class.getSimpleName(), key);
        turingMachine = tasksMap.get(key);
        Assert.assertNotNull(turingMachine);
        Assert.assertTrue(turingMachine.isScheduled());
        Assert.assertFalse(turingMachine.isLocked());
        Assert.assertFalse(turingMachine.isDone());

        startTimerForWorker();
        turingService.startAsWorker(TooringApplicationTests.class.getSimpleName());
        turingMachine = tasksMap.get(key);
        Assert.assertNotNull(turingMachine);
        Assert.assertFalse(turingMachine.isScheduled());
        Assert.assertFalse(turingMachine.isLocked());
        Assert.assertTrue(turingMachine.isDone());

        Optional<String> output = turingService.processOutput(key);
        Assert.assertTrue(output.isPresent());
        String json = output.get();
        DefaultTuringMachine outputTuringMachine = gson.fromJson(json, DefaultTuringMachine.class);
        Assert.assertNotNull(outputTuringMachine);
    }

    /**
     * Turns of worker after specified delay for unlocking current thread.
     */
    private void startTimerForWorker() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(BigInteger.ONE.intValue());
        executorService.schedule(() -> turingService.setWorker(false), BigInteger.TEN.longValue(), TimeUnit.MILLISECONDS);
    }

}
