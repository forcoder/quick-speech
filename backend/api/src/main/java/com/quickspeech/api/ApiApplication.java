package com.quickspeech.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.quickspeech")
@EntityScan(basePackages = "com.quickspeech")
@EnableJpaRepositories(basePackages = "com.quickspeech")
public class ApiApplication {

    public static void main(String[] args) {
        // Log ALL environment variables for debugging
        System.out.println("=== ENVIRONMENT VARIABLES ===");
        System.getenv().forEach((key, value) -> {
            if (key.contains("PASSWORD") || key.contains("SECRET") || key.contains("KEY")) {
                System.out.println("[ENV] " + key + "=" + value.substring(0, Math.min(4, value.length())) + "****");
            } else {
                System.out.println("[ENV] " + key + "=" + value);
            }
        });
        System.out.println("=== STARTING SPRING BOOT ===");
        try {
            SpringApplication.run(ApiApplication.class, args);
        } catch (Exception e) {
            System.err.println("=== STARTUP FAILED ===");
            System.err.println("Exception: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
