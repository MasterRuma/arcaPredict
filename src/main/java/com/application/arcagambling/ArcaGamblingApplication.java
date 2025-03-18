package com.application.arcagambling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArcaGamblingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArcaGamblingApplication.class, args);
    }

}
