package com.quickspeech.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;

@RestController
public class HealthController {

    @GetMapping(value = {"/", "/health", "/actuator/health"})
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "quick-speech-api");
        return result;
    }
}
