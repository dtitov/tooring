package com.uwc.tooring;

import com.google.gson.Gson;
import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.spring.context.SpringManagedContext;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.util.UuidUtil;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final String SCORES_MAP = "SCORES_MAP";
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
        } else {
            String id = cmd.getOptionValue(ID);
            startAsWorker(id);
        }
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
        String key = UuidUtil.newSecureUuidString();
        TransactionContext transactionContext = hazelcastInstance.newTransactionContext();
        transactionContext.beginTransaction();
        TransactionalMap<Object, Object> transactionalMap = transactionContext.getMap(TASKS_MAP);
        transactionalMap.put(key, inputTuringMachine, TASK_TTL_IN_HOURS, TimeUnit.HOURS);
        System.out.println("Key for submitted Turing machine is: " + key);
        System.out.println("Submitted task will expire in a number of hours: " + TASK_TTL_IN_HOURS);
        transactionContext.commitTransaction();
    }

    private void processOutput(String key, String fileName) {
        TransactionContext transactionContext = hazelcastInstance.newTransactionContext();
        transactionContext.beginTransaction();
        TransactionalMap<Object, Object> transactionalMap = transactionContext.getMap(TASKS_MAP);
        DefaultTuringMachine turingMachine = (DefaultTuringMachine) transactionalMap.get(key);
        Gson gson = new Gson();
        String json = gson.toJson(turingMachine);
        try {
            FileUtils.writeStringToFile(new File(fileName), json);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            transactionContext.rollbackTransaction();
            return;
        }
        transactionContext.commitTransaction();
    }

    private void scheduleExecution(String id, String key) {
        TransactionContext transactionContext = hazelcastInstance.newTransactionContext();
        transactionContext.beginTransaction();
        TransactionalMap<Object, Object> transactionalMap = transactionContext.getMap(TASKS_MAP);
        DefaultTuringMachine turingMachine = (DefaultTuringMachine) transactionalMap.getForUpdate(key);
        if (turingMachine.isScheduled()) {
            System.out.println("Computation is already scheduled for key: " + key);
            transactionContext.commitTransaction();
            return;
        }
        if (turingMachine.isRunning()) {
            System.out.println("Computation is already running for key: " + key);
            transactionContext.commitTransaction();
            return;
        }
        if (turingMachine.isDone()) {
            System.out.println("Computation is already done for key: " + key);
            transactionContext.commitTransaction();
            return;
        }
        turingMachine.schedule();
        decrementScore(id);
        System.out.println("Computation is scheduled for key: " + key);
        transactionContext.commitTransaction();
    }

    private void startAsWorker(String id) {
        while (true) {
            TransactionContext transactionContext = hazelcastInstance.newTransactionContext();
            transactionContext.beginTransaction();
            TransactionalMap<Object, Object> transactionalMap = transactionContext.getMap(TASKS_MAP);
            // TODO: implement worker logic
            transactionContext.commitTransaction();
            incrementScore(id);
        }
    }

    private void incrementScore(String id) {
        modifyScore(id, 1);
    }

    private void decrementScore(String id) {
        modifyScore(id, -1);
    }

    private void modifyScore(String id, int delta) {
        IMap<Object, Object> map = hazelcastInstance.getMap(SCORES_MAP);
        map.putIfAbsent(id, new AtomicInteger(BigInteger.ZERO.intValue()));
        AtomicInteger score = (AtomicInteger) map.get(id);
        if (delta < 0) {
            score.decrementAndGet();
        } else if (delta > 0) {
            score.incrementAndGet();
        }
    }

    private Options createCommandLineOptions() {
        Options options = new Options();

        Option set = new Option(SET, SET, false, "key for uploading Turing machine input");
        Option input = new Option(INPUT, INPUT, true, "filename of Turing machine description (JSON document) to upload");
        Option get = new Option(GET, GET, true, "key for downloading Turing machine output");
        Option output = new Option(OUTPUT, OUTPUT, true, "filename of Turing machine description (JSON document) to download to");
        Option schedule = new Option(SCHEDULE, SCHEDULE, true, "schedule Turing machine execution by specified key");
        Option worker = new Option(WORKER, WORKER, false, "start application as a worker (performer of computations)");

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
