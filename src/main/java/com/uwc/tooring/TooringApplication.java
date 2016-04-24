package com.uwc.tooring;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.cli.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.math.BigInteger;

/**
 * Main application class.
 */
@SpringBootApplication
@EnableScheduling
public class TooringApplication implements CommandLineRunner {

    private static final String SET = "set";
    private static final String INPUT = "input";
    private static final String GET = "get";
    private static final String OUTPUT = "output";
    private static final String SCHEDULE = "schedule";
    private static final String WORKER = "worker";
    private static final String ID = "id";

    private static final String HAZELCAST_LOGGING_TYPE = "hazelcast.logging.type";
    private static final String NONE = "none";

    @Autowired
    private TuringService turingService;

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
            turingService.processInputFile(fileName);
            System.exit(BigInteger.ZERO.intValue());
        } else if (cmd.hasOption(GET)) {
            String key = cmd.getOptionValue(GET);
            String fileName = cmd.getOptionValue(OUTPUT);
            turingService.processOutput(key, fileName);
            System.exit(BigInteger.ZERO.intValue());
        } else if (cmd.hasOption(SCHEDULE)) {
            String id = cmd.getOptionValue(ID);
            String key = cmd.getOptionValue(SCHEDULE);
            turingService.scheduleExecution(id, key);
            System.exit(BigInteger.ZERO.intValue());
        } else if (cmd.hasOption(WORKER)) {
            String id = cmd.getOptionValue(WORKER);
            turingService.startAsWorker(id);
        }
    }

    /**
     * Creates command line options for parsing console input.
     *
     * @return Command line options
     */
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
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setProperty(HAZELCAST_LOGGING_TYPE, NONE);

        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(5701);
        networkConfig.setPortAutoIncrement(true);

        return Hazelcast.newHazelcastInstance(config);
    }

}
