package com.uwc.tooring;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.ReplicatedMap;
import com.hazelcast.util.UuidUtil;
import com.uwc.tooring.turing.impl.DefaultTuringMachine;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class TuringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TuringService.class);

    public static final int TASK_TTL_IN_HOURS = 12;

    public static final String TASKS_MAP = "TASKS_MAP";

    public static final long WORKER_RATE = 1000L;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    private volatile boolean worker;

    /**
     * Processes input JSON file with Turing machine description.
     *
     * @param fileName Input file name
     * @throws IOException If file can't be accessed
     */
    public void processInputFile(String fileName) throws IOException {
        String json;
        json = FileUtils.readFileToString(new File(fileName));
        String key = processInputJSON(json);
        System.out.println("Key for submitted Turing machine is: " + key);
        System.out.println("Submitted task will expire in a number of hours: " + TASK_TTL_IN_HOURS);
    }

    /**
     * Processes input JSON string with Turing machine description.
     *
     * @param json String with JSON description of Turing machine
     * @return key for submitted Turing machine
     */
    public String processInputJSON(String json) {
        Gson gson = new Gson();
        DefaultTuringMachine inputTuringMachine = gson.fromJson(json, DefaultTuringMachine.class);
        String key = UuidUtil.newSecureUuidString();
        ReplicatedMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getReplicatedMap(TASKS_MAP);
        tasksMap.put(key, inputTuringMachine, TASK_TTL_IN_HOURS, TimeUnit.HOURS);
        return key;
    }

    /**
     * Saves output to file.
     *
     * @param key      Key of Turing machine
     * @param fileName File name to save output to
     * @throws IOException If file can't be created
     */
    public void processOutput(String key, String fileName) throws IOException {
        Optional<String> output = processOutput(key);
        if (output.isPresent()) {
            FileUtils.writeStringToFile(new File(fileName), output.get());
        } else {
            System.out.println("There's no Turing machine with the specified key or it's not ready yet.");
        }
    }

    /**
     * Returns output as JSON String.
     *
     * @param key Key of Turing machine
     * @return String with JSON representation of Turing machine
     */
    public Optional<String> processOutput(String key) {
        ReplicatedMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getReplicatedMap(TASKS_MAP);
        if (!tasksMap.containsKey(key)) {
            return Optional.empty();
        }
        DefaultTuringMachine turingMachine = tasksMap.get(key);
        if (!turingMachine.isDone()) {
            return Optional.empty();
        }
        tasksMap.remove(key);
        Gson gson = new Gson();
        return Optional.of(gson.toJson(turingMachine));
    }

    /**
     * Schedules processing of Turing machine.
     *
     * @param id  User ID (for counting score)
     * @param key The key of Turing machine submitted previously
     */
    public void scheduleExecution(String id, String key) {
        ILock lock = hazelcastInstance.getLock(key);
        if (lock.tryLock()) {
            try {
                ReplicatedMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getReplicatedMap(TASKS_MAP);
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
                turingMachine.schedule(id);
                tasksMap.put(key, turingMachine);
                decrementScore(id);
                System.out.println("Computation is scheduled for the Turing machine with specified key.");
            } finally {
                lock.forceUnlock();
            }
        } else {
            System.out.println("Can't schedule Turing machine with specified key, because it's locked by some operation. Try again a bit later.");
        }
    }

    /**
     * Becomes a worker application.
     *
     * @param id User ID (for counting score)
     */
    public void startAsWorker(String id) {
        setWorker(true);
        while (isWorker()) {
            Optional<Map.Entry<String, DefaultTuringMachine>> turingMachineToProcess = getTuringMachineToProcess();
            turingMachineToProcess.ifPresent(turingMachine -> processTuringMachine(id, turingMachine));
            try {
                Thread.sleep(WORKER_RATE); // sleep a bit between attempts
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
        ReplicatedMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getReplicatedMap(TASKS_MAP);
        Set<Map.Entry<String, DefaultTuringMachine>> entries = tasksMap.entrySet();

        return entries.stream().filter(e -> {
            DefaultTuringMachine machine = e.getValue();
            return machine.isScheduled() && !machine.isLocked() && !machine.isDone();
        }).max((left, right) -> {
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
        if (lock.tryLock()) {
            try {
                ReplicatedMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getReplicatedMap(TASKS_MAP);
                DefaultTuringMachine turingMachine = tasksMap.get(key);
                turingMachine.run(true);
                tasksMap.put(key, turingMachine);
                incrementScore(id);
                LOGGER.info("Turing machine was successfully computed, key = " + key);
            } finally {
                lock.forceUnlock();
            }
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

    /**
     * Detects if current node is "worker" node
     *
     * @return true if current node is "worker" node, false otherwise
     */
    public boolean isWorker() {
        return worker;
    }

    /**
     * Sets worker flag.
     *
     * @param worker true if current node is "worker" node, false otherwise
     */
    public void setWorker(boolean worker) {
        this.worker = worker;
    }

}
