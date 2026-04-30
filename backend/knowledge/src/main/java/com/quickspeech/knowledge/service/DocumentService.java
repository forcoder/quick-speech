package com.quickspeech.knowledge.service;

import com.quickspeech.common.constant.ResponseCode;
import com.quickspeech.common.entity.PageResult;
import com.quickspeech.common.exception.BusinessException;
import com.quickspeech.common.util.JwtUtil;
import com.quickspeech.knowledge.entity.DocumentChunk;
import com.quickspeech.knowledge.entity.KnowledgeBase;
import com.quickspeech.knowledge.entity.KnowledgeDocument;
import com.quickspeech.knowledge.repository.DocumentChunkRepository;
import com.quickspeech.knowledge.repository.KnowledgeDocumentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final JwtUtil jwtUtil;

    public DocumentService(KnowledgeDocumentRepository documentRepository,
                           DocumentChunkRepository chunkRepository,
                           KnowledgeBaseService knowledgeBaseService,
                           JwtUtil jwtUtil) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public KnowledgeDocument uploadDocument(Long knowledgeBaseId, String title, String fileName,
                                             String fileType, Long fileSize, String filePath,
                                             String content, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        // 验证知识库存在
        knowledgeBaseService.getById(knowledgeBaseId, request);

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTenantId(tenantId);
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setTitle(title);
        doc.setFileName(fileName);
        doc.setFileType(fileType);
        doc.setFileSize(fileSize);
        doc.setFilePath(filePath);
        doc.setContent(content);
        doc.setStatus("PENDING");
        doc.setChunkCount(0);
        doc.setCreatedBy(getUserId(request));
        return documentRepository.save(doc);
    }

    public KnowledgeDocument getById(Long id, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        return documentRepository.findByIdAndDeletedFalse(id)
                .filter(doc -> doc.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ResponseCode.DOCUMENT_NOT_FOUND));
    }

    public PageResult<KnowledgeDocument> listByKnowledgeBase(Long knowledgeBaseId, int page, int pageSize,
                                                              HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        Page<KnowledgeDocument> result = documentRepository.findAllByTenantIdAndKnowledgeBaseIdAndDeletedFalse(
                tenantId, knowledgeBaseId,
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(result.getContent(), result.getTotalElements(), page, pageSize);
    }

    @Transactional
    public KnowledgeDocument update(Long id, String title, String content, HttpServletRequest request) {
        KnowledgeDocument existing = getById(id, request);
        if (title != null) existing.setTitle(title);
        if (content != null) {
            existing.setContent(content);
            existing.setStatus("PENDING");
        }
        existing.setUpdatedBy(getUserId(request));
        return documentRepository.save(existing);
    }

    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        KnowledgeDocument existing = getById(id, request);
        // 软删除关联的分块
        chunkRepository.softDeleteByDocumentId(existing.getTenantId(), id);
        existing.setDeleted(true);
        existing.setUpdatedBy(getUserId(request));
        documentRepository.save(existing);
    }

    public List<DocumentChunk> getChunks(Long documentId, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        KnowledgeDocument doc = getById(documentId, request);
        return chunkRepository.findAllByTenantIdAndDocumentIdAndDeletedFalseOrderByChunkIndexAsc(
                tenantId, documentId);
    }

    @Transactional
    public List<DocumentChunk> createChunks(Long documentId, List<String> chunkContents, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        KnowledgeDocument doc = getById(documentId, request);

        List<DocumentChunk> chunks = new java.util.ArrayList<>();
        for (int i = 0; i < chunkContents.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setTenantId(tenantId);
            chunk.setDocumentId(documentId);
            chunk.setKnowledgeBaseId(doc.getKnowledgeBaseId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunkContents.get(i));
            chunk.setContentHash(String.valueOf(chunkContents.get(i).hashCode()));
            chunk.setWordCount(chunkContents.get(i).split("\\s+").length);
            chunk.setCharCount(chunkContents.get(i).length());
            chunk.setIsVectorized(false);
            chunk.setCreatedBy(getUserId(request));
            chunks.add(chunk);
        }
        List<DocumentChunk> saved = chunkRepository.saveAll(chunks);

        doc.setChunkCount(saved.size());
        doc.setStatus("CHUNKED");
        documentRepository.save(doc);

        return saved;
    }

    private Long getTenantId(HttpServletRequest request) {
        String token = extractToken(request);
        return jwtUtil.getTenantId(token);
    }

    private Long getUserId(HttpServletRequest request) {
        String token = extractToken(request);
        return jwtUtil.getUserId(token);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
