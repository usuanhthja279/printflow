package com.printflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class PrintFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(PrintFlowApplication.class, args);
    }
}
