package com.uwc.tooring;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.context.SpringManagedContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Main application class
 */
@SpringBootApplication
public class TooringApplication {

    public static void main(String[] args) {
        SpringApplication.run(TooringApplication.class, args);
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

        config.getQueueConfig(TooringApplication.class.getSimpleName()).setMaxSize(Integer.MAX_VALUE);

        return Hazelcast.newHazelcastInstance(config);
    }

}
