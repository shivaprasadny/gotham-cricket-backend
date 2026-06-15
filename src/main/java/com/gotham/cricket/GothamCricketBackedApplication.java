package com.gotham.cricket;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition
@SpringBootApplication
@EnableScheduling
public class GothamCricketBackedApplication {

    public static void main(String[] args) {
        SpringApplication.run(GothamCricketBackedApplication.class, args);
    }
}