package com.uwc.tooring;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.uwc.tooring.turing.impl.DefaultTuringMachine;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class TuringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TuringService.class);

    private static final int TASK_TTL_IN_HOURS = 12;

    private static final String TASKS_MAP = "TASKS_MAP";

    @Autowired
    private HazelcastInstance hazelcastInstance;

    public void processInput(String fileName) {
        String json;
        try {
            json = FileUtils.readFileToString(new File(fileName));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }
        Gson gson = new Gson();
        DefaultTuringMachine inputTuringMachine = gson.fromJson(json, DefaultTuringMachine.class);
        String key = Long.toString(hazelcastInstance.getIdGenerator(TooringApplication.class.getSimpleName()).newId());
        IMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getMap(TASKS_MAP);
        tasksMap.putIfAbsent(key, inputTuringMachine, TASK_TTL_IN_HOURS, TimeUnit.HOURS);
        System.out.println("Key for submitted Turing machine is: " + key);
        System.out.println("Submitted task will expire in a number of hours: " + TASK_TTL_IN_HOURS);
    }

    public void processOutput(String key, String fileName) throws IOException {
        IMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getMap(TASKS_MAP);
        if (!tasksMap.containsKey(key)) {
            System.out.println("There's no Turing machine with the specified key.");
            return;
        }
        DefaultTuringMachine turingMachine = tasksMap.get(key);
        Gson gson = new Gson();
        String json = gson.toJson(turingMachine);
        FileUtils.writeStringToFile(new File(fileName), json);
    }

    public void scheduleExecution(String id, String key) {
        IMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getMap(TASKS_MAP);
        boolean lockAcquired;
        try {
            lockAcquired = tasksMap.tryLock(key, BigInteger.ONE.longValue(), TimeUnit.NANOSECONDS, BigInteger.TEN.longValue(), TimeUnit.SECONDS);
            if (lockAcquired) {
                DefaultTuringMachine turingMachine = tasksMap.get(key);
                if (turingMachine == null) {
                    System.out.println("There's no Turing machine with specified key.");
                    return;
                }
                if (turingMachine.isScheduled()) {
                    System.out.println("Computation is already scheduled for the Turing machine with specified key.");
                    return;
                }
                if (turingMachine.isRunning()) {
                    System.out.println("Computation is already running for the Turing machine with specified key.");
                    return;
                }
                if (turingMachine.isDone()) {
                    System.out.println("Computation is already done for the Turing machine with specified key.");
                    return;
                }
                turingMachine.schedule(id);
                tasksMap.put(key, turingMachine);
                decrementScore(id);
                System.out.println("Computation is scheduled for the Turing machine with specified key.");
            } else {
                System.out.println("Can't schedule Turing machine with specified key, because it's locked by some operation. Try again a bit later.");
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            tasksMap.unlock(key);
        }
    }

    public void startAsWorker(String id) {
        while (true) {
            Optional<Map.Entry<String, DefaultTuringMachine>> turingMachineToProcess = getTuringMachineToProcess();
            turingMachineToProcess.ifPresent(turingMachine -> processTuringMachine(id, turingMachine));
            try {
                Thread.sleep(BigInteger.TEN.longValue());
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private Optional<Map.Entry<String, DefaultTuringMachine>> getTuringMachineToProcess() {
        IMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getMap(TASKS_MAP);

        EntryObject value = new PredicateBuilder().getEntryObject();
        Predicate predicate = value.is("scheduled").and(value.isNot("running")).and(value.isNot("done"));
        Set<Map.Entry<String, DefaultTuringMachine>> entries = tasksMap.entrySet(predicate);

        return entries.stream().max((left, right) -> {
            DefaultTuringMachine leftValue = left.getValue();
            DefaultTuringMachine rightValue = right.getValue();
            long x = getScore(leftValue.getId());
            long y = getScore(rightValue.getId());
            return Long.compare(x, y);
        });
    }

    private void processTuringMachine(String id, Map.Entry<String, DefaultTuringMachine> turingMachineEntry) {
        String key = turingMachineEntry.getKey();
        IMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getMap(TASKS_MAP);
        boolean lockAcquired;
        try {
            lockAcquired = tasksMap.tryLock(key, BigInteger.ONE.longValue(), TimeUnit.SECONDS, BigInteger.TEN.longValue(), TimeUnit.SECONDS);
            if (lockAcquired) {
                DefaultTuringMachine turingMachine = tasksMap.get(key);
                turingMachine.run(true);
                tasksMap.put(key, turingMachine);
                incrementScore(id);
                LOGGER.info("Turing machine was successfully computed, key = " + key);
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            tasksMap.unlock(key);
        }
    }

    private void incrementScore(String id) {
        hazelcastInstance.getAtomicLong(id).incrementAndGet();
    }

    private void decrementScore(String id) {
        hazelcastInstance.getAtomicLong(id).decrementAndGet();
    }

    private long getScore(String id) {
        return hazelcastInstance.getAtomicLong(id).get();
    }

}
