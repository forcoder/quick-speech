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
        // Log environment variables for debugging
        String[] envVars = {"DB_HOST", "DB_PORT", "DB_NAME", "DB_USERNAME", "PORT", "JWT_SECRET"};
        for (String env : envVars) {
            String value = System.getenv(env);
            if (value != null) {
                // Mask password-like values
                if (env.contains("PASSWORD") || env.contains("SECRET") || env.contains("KEY")) {
                    System.out.println("[ENV] " + env + "=" + value.substring(0, Math.min(4, value.length())) + "****");
                } else {
                    System.out.println("[ENV] " + env + "=" + value);
                }
            } else {
                System.out.println("[ENV] " + env + " is NOT SET!");
            }
        }
        SpringApplication.run(ApiApplication.class, args);
    }
}
