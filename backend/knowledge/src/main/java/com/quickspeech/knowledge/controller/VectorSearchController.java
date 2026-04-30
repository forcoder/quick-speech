package com.quickspeech.knowledge.controller;

import com.quickspeech.common.entity.ApiResponse;
import com.quickspeech.knowledge.service.VectorSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vector")
public class VectorSearchController {

    private final VectorSearchService vectorSearchService;

    public VectorSearchController(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    @PostMapping("/search")
    public ApiResponse<List<Map<String, Object>>> search(@RequestBody Map<String, Object> request) {
        String collection = (String) request.get("collection");
        String query = (String) request.get("query");
        int topK = request.get("topK") != null ? (int) request.get("topK") : 5;
        return ApiResponse.success(vectorSearchService.search(collection, query, topK));
    }

    @PostMapping("/embedding")
    public ApiResponse<List<Double>> getEmbedding(@RequestBody Map<String, String> request) {
        return ApiResponse.success(vectorSearchService.getEmbedding(request.get("text")));
    }

    @PostMapping("/embeddings")
    public ApiResponse<List<List<Double>>> getEmbeddings(@RequestBody Map<String, List<String>> request) {
        return ApiResponse.success(vectorSearchService.getEmbeddings(request.get("texts")));
    }

    @PostMapping("/upsert")
    public ApiResponse<Void> upsert(@RequestBody Map<String, Object> request) {
        String collection = (String) request.get("collection");
        List<String> ids = (List<String>) request.get("ids");
        List<List<Double>> embeddings = (List<List<Double>>) request.get("embeddings");
        List<String> documents = (List<String>) request.get("documents");
        List<Map<String, Object>> metadatas = (List<Map<String, Object>>) request.get("metadatas");
        vectorSearchService.upsertVectors(collection, ids, embeddings, documents, metadatas);
        return ApiResponse.success();
    }
}
