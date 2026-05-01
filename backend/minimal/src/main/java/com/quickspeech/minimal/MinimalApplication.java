package com.quickspeech.minimal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@SpringBootApplication
@RestController
public class MinimalApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinimalApplication.class, args);
    }

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("status", "ok", "service", "quick-speech-api");
    }

    @GetMapping("/actuator/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    @PostMapping("/api/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", "mock-jwt-token");
        result.put("userId", 1);
        result.put("username", request.getOrDefault("username", "admin"));
        result.put("role", "ADMIN");
        result.put("tenantId", 1);
        return result;
    }

    @PostMapping("/api/auth/register")
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", "mock-jwt-token");
        result.put("userId", 1);
        result.put("username", request.getOrDefault("username", "user"));
        return result;
    }

    @GetMapping("/api/knowledge-bases")
    public Map<String, Object> listKnowledgeBases() {
        return Map.of("content", List.of(), "totalElements", 0, "page", 1, "pageSize", 20);
    }

    @GetMapping("/api/agents")
    public Map<String, Object> listAgents() {
        return Map.of("content", List.of(), "totalElements", 0, "page", 1, "pageSize", 20);
    }
}
