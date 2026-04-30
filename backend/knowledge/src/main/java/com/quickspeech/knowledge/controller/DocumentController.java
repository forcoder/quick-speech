package com.quickspeech.knowledge.controller;

import com.quickspeech.common.entity.ApiResponse;
import com.quickspeech.common.entity.PageResult;
import com.quickspeech.knowledge.entity.DocumentChunk;
import com.quickspeech.knowledge.entity.KnowledgeDocument;
import com.quickspeech.knowledge.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ApiResponse<KnowledgeDocument> upload(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        KnowledgeDocument doc = documentService.uploadDocument(
                Long.valueOf(payload.get("knowledgeBaseId").toString()),
                (String) payload.get("title"),
                (String) payload.getOrDefault("fileName", ""),
                (String) payload.getOrDefault("fileType", ""),
                payload.get("fileSize") != null ? Long.valueOf(payload.get("fileSize").toString()) : null,
                (String) payload.getOrDefault("filePath", ""),
                (String) payload.getOrDefault("content", ""),
                request);
        return ApiResponse.success(doc);
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeDocument> getById(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(documentService.getById(id, request));
    }

    @GetMapping("/knowledge-base/{kbId}")
    public ApiResponse<PageResult<KnowledgeDocument>> listByKnowledgeBase(
            @PathVariable Long kbId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(documentService.listByKnowledgeBase(kbId, page, pageSize, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeDocument> update(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates,
            HttpServletRequest request) {
        return ApiResponse.success(documentService.update(
                id, updates.get("title"), updates.get("content"), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        documentService.delete(id, request);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/chunks")
    public ApiResponse<List<DocumentChunk>> getChunks(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(documentService.getChunks(id, request));
    }

    @PostMapping("/{id}/chunks")
    public ApiResponse<List<DocumentChunk>> createChunks(
            @PathVariable Long id,
            @RequestBody Map<String, List<String>> body,
            HttpServletRequest request) {
        return ApiResponse.success(documentService.createChunks(id, body.get("chunks"), request));
    }
}
