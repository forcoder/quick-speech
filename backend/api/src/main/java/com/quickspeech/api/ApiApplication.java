package com.quickspeech.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.quickspeech")
public class ApiApplication {

    public static void main(String[] args) {
        System.out.println("=== STARTING QUICK SPEECH API ===");
        System.out.println("PORT=" + System.getenv("PORT"));
        try {
            SpringApplication.run(ApiApplication.class, args);
            System.out.println("=== SPRING BOOT STARTED SUCCESSFULLY ===");
        } catch (Exception e) {
            System.err.println("=== STARTUP FAILED ===");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
