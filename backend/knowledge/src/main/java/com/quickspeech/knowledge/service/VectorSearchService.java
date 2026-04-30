package com.quickspeech.knowledge.service;

import com.quickspeech.common.exception.BusinessException;
import com.quickspeech.common.constant.ResponseCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class VectorSearchService {

    @Value("${embedding.api.url:https://api.openai.com/v1/embeddings}")
    private String embeddingApiUrl;

    @Value("${embedding.api.key:}")
    private String embeddingApiKey;

    @Value("${embedding.model:text-embedding-ada-002}")
    private String embeddingModel;

    @Value(${chroma.url:http://localhost:8000})
    private String chromaUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 获取文本的向量嵌入
     */
    public List<Double> getEmbedding(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (embeddingApiKey != null && !embeddingApiKey.isEmpty()) {
                headers.setBearerAuth(embeddingApiKey);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("input", text);
            body.put("model", embeddingModel);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(embeddingApiUrl, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                if (data != null && !data.isEmpty()) {
                    return (List<Double>) data.get(0).get("embedding");
                }
            }
            throw new BusinessException(ResponseCode.EMBEDDING_ERROR);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.EMBEDDING_ERROR,
                    "向量化失败: " + e.getMessage());
        }
    }

    /**
     * 批量获取向量嵌入
     */
    public List<List<Double>> getEmbeddings(List<String> texts) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (embeddingApiKey != null && !embeddingApiKey.isEmpty()) {
                headers.setBearerAuth(embeddingApiKey);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("input", texts);
            body.put("model", embeddingModel);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(embeddingApiUrl, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                if (data != null) {
                    List<List<Double>> embeddings = new ArrayList<>();
                    for (Map<String, Object> item : data) {
                        embeddings.add((List<Double>) item.get("embedding"));
                    }
                    return embeddings;
                }
            }
            throw new BusinessException(ResponseCode.EMBEDDING_ERROR);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.EMBEDDING_ERROR,
                    "批量向量化失败: " + e.getMessage());
        }
    }

    /**
     * 在Chroma中搜索相似文档
     */
    public List<Map<String, Object>> search(String collectionName, String query, int topK) {
        try {
            List<Double> queryEmbedding = getEmbedding(query);

            String url = chromaUrl + "/api/v1/collections/" + collectionName + "/query";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("query_embeddings", List.of(queryEmbedding));
            body.put("n_results", topK);
            body.put("include", List.of("documents", "metadatas", "distances"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseSearchResults(response.getBody());
            }
            throw new BusinessException(ResponseCode.VECTOR_DB_ERROR);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.VECTOR_DB_ERROR,
                    "向量搜索失败: " + e.getMessage());
        }
    }

    /**
     * 向Chroma中插入向量
     */
    public void upsertVectors(String collectionName, List<String> ids,
                               List<List<Double>> embeddings, List<String> documents,
                               List<Map<String, Object>> metadatas) {
        try {
            String url = chromaUrl + "/api/v1/collections/" + collectionName + "/upsert";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("ids", ids);
            body.put("embeddings", embeddings);
            body.put("documents", documents);
            body.put("metadatas", metadatas);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, Map.class);
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.VECTOR_DB_ERROR,
                    "向量插入失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseSearchResults(Map<String, Object> responseBody) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<List<String>> documents = (List<List<String>>) responseBody.get("documents");
        List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) responseBody.get("metadatas");
        List<List<Double>> distances = (List<List<Double>>) responseBody.get("distances");
        List<List<String>> ids = (List<List<String>>) responseBody.get("ids");

        if (documents != null && !documents.isEmpty()) {
            for (int i = 0; i < documents.get(0).size(); i++) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", ids.get(0).get(i));
                result.put("document", documents.get(0).get(i));
                result.put("metadata", metadatas.get(0).get(i));
                result.put("distance", distances.get(0).get(i));
                result.put("score", 1.0 - distances.get(0).get(i));
                results.add(result);
            }
        }
        return results;
    }
}
