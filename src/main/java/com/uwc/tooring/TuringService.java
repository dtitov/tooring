package com.uwc.tooring;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
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

    public static final String SCHEDULED = "scheduled";
    public static final String LOCKED = "locked";
    public static final String DONE = "done";

    @Autowired
    private HazelcastInstance hazelcastInstance;

    /**
     * Processes input JSON file with Turing machine description.
     *
     * @param fileName Input file name
     * @throws IOException If file can't be located
     */
    public void processInput(String fileName) throws IOException {
        String json;
        json = FileUtils.readFileToString(new File(fileName));

        Gson gson = new Gson();
        DefaultTuringMachine inputTuringMachine = gson.fromJson(json, DefaultTuringMachine.class);
        String key = Long.toString(hazelcastInstance.getIdGenerator(TooringApplication.class.getSimpleName()).newId());
        IMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getMap(TASKS_MAP);
        tasksMap.putIfAbsent(key, inputTuringMachine, TASK_TTL_IN_HOURS, TimeUnit.HOURS);
        System.out.println("Key for submitted Turing machine is: " + key);
        System.out.println("Submitted task will expire in a number of hours: " + TASK_TTL_IN_HOURS);
    }

    /**
     * Saves output to file.
     *
     * @param key      Key of Turing machine
     * @param fileName File name to save output to
     * @throws IOException If file can't be created
     */
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

    /**
     * Schedules processing of Turing machine.
     *
     * @param id  User ID (for counting score)
     * @param key The key of Turing machine submitted previously
     */
    public void scheduleExecution(String id, String key) {
        ILock lock = hazelcastInstance.getLock(key);
        boolean lockAcquired;
        try {
            lockAcquired = lock.tryLock();
            if (lockAcquired) {
                IMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getMap(TASKS_MAP);
                DefaultTuringMachine turingMachine = tasksMap.get(key);
                if (turingMachine == null) {
                    System.out.println("There's no Turing machine with specified key.");
                    return;
                }
                if (turingMachine.isScheduled()) {
                    System.out.println("Computation is already scheduled for the Turing machine with specified key.");
                    return;
                }
                if (turingMachine.isDone()) {
                    System.out.println("Computation is already done for the Turing machine with specified key.");
                    return;
                }
                turingMachine.schedule(id, lock);
                tasksMap.put(key, turingMachine);
                decrementScore(id);
                System.out.println("Computation is scheduled for the Turing machine with specified key.");
            } else {
                System.out.println("Can't schedule Turing machine with specified key, because it's locked by some operation. Try again a bit later.");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Becomes a worker application.
     *
     * @param id User ID (for counting score)
     */
    public void startAsWorker(String id) {
        while (true) {
            Optional<Map.Entry<String, DefaultTuringMachine>> turingMachineToProcess = getTuringMachineToProcess();
            turingMachineToProcess.ifPresent(turingMachine -> processTuringMachine(id, turingMachine));
            try {
                Thread.sleep(BigInteger.TEN.longValue()); // sleep a bit between attempts
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Finds next Turing machine waiting for being processed.
     *
     * @return Next Turing machine to process (with it's key)
     */
    private Optional<Map.Entry<String, DefaultTuringMachine>> getTuringMachineToProcess() {
        IMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getMap(TASKS_MAP);

        EntryObject value = new PredicateBuilder().getEntryObject();
        Predicate predicate = value.is(SCHEDULED).and(value.isNot(LOCKED)).and(value.isNot(DONE));
        Set<Map.Entry<String, DefaultTuringMachine>> entries = tasksMap.entrySet(predicate);

        return entries.stream().max((left, right) -> {
            DefaultTuringMachine leftValue = left.getValue();
            DefaultTuringMachine rightValue = right.getValue();
            long x = getScore(leftValue.getId());
            long y = getScore(rightValue.getId());
            return Long.compare(x, y);
        });
    }

    /**
     * Performs computations on the Turing machine.
     *
     * @param id                 User ID (for counting score)
     * @param turingMachineEntry Turing machine to compute
     */
    private void processTuringMachine(String id, Map.Entry<String, DefaultTuringMachine> turingMachineEntry) {
        String key = turingMachineEntry.getKey();
        ILock lock = hazelcastInstance.getLock(key);
        boolean lockAcquired;
        try {
            lockAcquired = lock.tryLock();
            if (lockAcquired) {
                IMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getMap(TASKS_MAP);
                DefaultTuringMachine turingMachine = tasksMap.get(key);
                turingMachine.run(lock, true);
                tasksMap.put(key, turingMachine);
                incrementScore(id);
                LOGGER.info("Turing machine was successfully computed, key = " + key);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Increments user's score
     *
     * @param id User ID
     */
    private void incrementScore(String id) {
        hazelcastInstance.getAtomicLong(id).incrementAndGet();
    }

    /**
     * Decrements user's score
     *
     * @param id User ID
     */
    private void decrementScore(String id) {
        hazelcastInstance.getAtomicLong(id).decrementAndGet();
    }

    /**
     * Gets user's score
     *
     * @param id User ID
     */
    private long getScore(String id) {
        return hazelcastInstance.getAtomicLong(id).get();
    }

}
