package com.quickspeech.knowledge.service;

import com.quickspeech.common.exception.BusinessException;
import com.quickspeech.common.constant.ResponseCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChromaCollectionService {

    @Value("${chroma.url:http://localhost:8000}")
    private String chromaUrl;

    private final RestTemplate restTemplate;

    public ChromaCollectionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 创建集合
     */
    public void createCollection(String collectionName) {
        try {
            String url = chromaUrl + "/api/v1/collections";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("name", collectionName);
            body.put("get_or_create", true);
            body.put("metadata", Map.of("hnsw:space", "cosine"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, Map.class);
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.VECTOR_DB_ERROR,
                    "创建集合失败: " + e.getMessage());
        }
    }

    /**
     * 删除集合
     */
    public void deleteCollection(String collectionName) {
        try {
            String url = chromaUrl + "/api/v1/collections/" + collectionName;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.VECTOR_DB_ERROR,
                    "删除集合失败: " + e.getMessage());
        }
    }

    /**
     * 获取集合信息
     */
    public Map<String, Object> getCollection(String collectionName) {
        try {
            String url = chromaUrl + "/api/v1/collections/" + collectionName;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            return new HashMap<>();
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.VECTOR_DB_ERROR,
                    "获取集合失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有集合
     */
    @SuppressWarnings("unchecked")
    public java.util.List<Map<String, Object>> listCollections() {
        try {
            String url = chromaUrl + "/api/v1/collections";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (java.util.List<Map<String, Object>>) response.getBody();
            }
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.VECTOR_DB_ERROR,
                    "列出集合失败: " + e.getMessage());
        }
    }
}
