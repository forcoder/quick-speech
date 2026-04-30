package com.quickspeech.knowledge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ChromaConfig {

    @Value("${chroma.url:http://localhost:8000}")
    private String chromaUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getChromaUrl() {
        return chromaUrl;
    }
}
