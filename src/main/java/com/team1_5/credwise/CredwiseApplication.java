package com.team1_5.credwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.team1_5.credwise.repository")
public class CredwiseApplication {
    public static void main(String[] args) {
        SpringApplication.run(CredwiseApplication.class, args);
    }
}

