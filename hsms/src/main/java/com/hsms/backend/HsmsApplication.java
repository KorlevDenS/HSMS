package com.hsms.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

//Later for async events
@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
public class HsmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HsmsApplication.class, args);
    }

}
