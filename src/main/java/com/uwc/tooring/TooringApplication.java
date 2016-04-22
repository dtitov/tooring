package com.uwc.tooring;

import com.google.gson.Gson;
import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.spring.context.SpringManagedContext;
import com.uwc.tooring.turing.impl.DefaultTuringMachine;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Main application class.
 */
@SpringBootApplication
public class TooringApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TooringApplication.class);

    private static final String SET = "set";
    private static final String INPUT = "input";

    private static final String GET = "get";
    private static final String OUTPUT = "output";

    private static final String SCHEDULE = "schedule";

    private static final String WORKER = "worker";

    private static final String ID = "id";

    private static final int TASK_TTL_IN_HOURS = 12;

    private static final String TASKS_MAP = "TASKS_MAP";

    @Autowired
    HazelcastInstance hazelcastInstance;

    public static void main(String[] args) throws ParseException {
        SpringApplication.run(TooringApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Options options = createCommandLineOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(SET)) {
            String fileName = cmd.getOptionValue(INPUT);
            processInput(fileName);
        } else if (cmd.hasOption(GET)) {
            String key = cmd.getOptionValue(GET);
            String fileName = cmd.getOptionValue(OUTPUT);
            processOutput(key, fileName);
        } else if (cmd.hasOption(SCHEDULE)) {
            String id = cmd.getOptionValue(ID);
            String key = cmd.getOptionValue(SCHEDULE);
            scheduleExecution(id, key);
        } else if (cmd.hasOption(WORKER)) {
            String id = cmd.getOptionValue(WORKER);
            startAsWorker(id);
        }
        System.exit(BigInteger.ZERO.intValue());
    }

    private void processInput(String fileName) {
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

    private void processOutput(String key, String fileName) throws IOException {
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

    private void scheduleExecution(String id, String key) {
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

    private void startAsWorker(String id) {
        while (true) {
            Optional<Map.Entry<String, DefaultTuringMachine>> turingMachineToProcess = getTuringMachineToProcess();
            turingMachineToProcess.ifPresent(turingMachine -> processTuringMachine(id, turingMachine));
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

    private Options createCommandLineOptions() {
        Options options = new Options();

        Option set = new Option(SET, SET, false, "key for uploading Turing machine input");
        Option input = new Option(INPUT, INPUT, true, "filename of Turing machine description (JSON document) to upload");
        Option get = new Option(GET, GET, true, "key for downloading Turing machine output");
        Option output = new Option(OUTPUT, OUTPUT, true, "filename of Turing machine description (JSON document) to download to");
        Option schedule = new Option(SCHEDULE, SCHEDULE, true, "schedule Turing machine execution by specified key");
        Option worker = new Option(WORKER, WORKER, true, "start application as a worker (performer of computations) with specified ID");

        Option id = new Option(ID, ID, true, "identificator of user (arbitrary string) for defining it's score");

        OptionGroup mainOptionGroup = new OptionGroup();
        mainOptionGroup.addOption(set);
        mainOptionGroup.addOption(get);
        mainOptionGroup.addOption(schedule);
        mainOptionGroup.addOption(worker);

        OptionGroup additionalOptionGroup = new OptionGroup();
        additionalOptionGroup.addOption(input);
        additionalOptionGroup.addOption(output);

        options.addOptionGroup(mainOptionGroup);
        options.addOptionGroup(additionalOptionGroup);

        options.addOption(id);

        return options;
    }

    @Bean
    public SpringManagedContext managedContext() {
        return new SpringManagedContext();
    }

    @Bean
    public HazelcastInstance hazelcastInstance(@Value("${hazelcast.discovery:LAN}") String hazelcastDiscovery,
                                               @Value("${aws.access.key:}") String accessKey,
                                               @Value("${aws.secret.key:}") String secretKey) {
        Config config = new Config();
        config.setProperty("hazelcast.logging.type", "none");
        config.setManagedContext(managedContext());

        NetworkConfig networkConfig = config.getNetworkConfig();
        switch (hazelcastDiscovery.toUpperCase()) {
            case "AWS":
                networkConfig.getJoin().getAwsConfig().setAccessKey(accessKey).setSecretKey(secretKey).setEnabled(true);
                networkConfig.getJoin().getMulticastConfig().setEnabled(false);
                break;
            case "LAN":
                networkConfig.setPort(5701);
                networkConfig.setPortAutoIncrement(true);
                break;
            default:
                throw new RuntimeException("Property \"hazelcast.discovery\" is wrong (available values are LAN and AWS");
        }

        config.getQueueConfig(TASKS_MAP).setMaxSize(Integer.MAX_VALUE);

        return Hazelcast.newHazelcastInstance(config);
    }

}
