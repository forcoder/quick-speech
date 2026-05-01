package com.quickspeech.minimal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SpringBootApplication
@RestController
public class MinimalApplication {

    public static void main(String[] args) {
        System.out.println("=== MINIMAL APP STARTING ===");
        SpringApplication.run(MinimalApplication.class, args);
        System.out.println("=== MINIMAL APP STARTED ===");
    }

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("status", "ok");
    }

    @GetMapping("/actuator/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
