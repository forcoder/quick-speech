package com.quickspeech.knowledge.service;

import com.quickspeech.common.constant.ResponseCode;
import com.quickspeech.common.entity.PageResult;
import com.quickspeech.common.exception.BusinessException;
import com.quickspeech.common.util.JwtUtil;
import com.quickspeech.knowledge.entity.KnowledgeBase;
import com.quickspeech.knowledge.repository.KnowledgeBaseRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final JwtUtil jwtUtil;

    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository, JwtUtil jwtUtil) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public KnowledgeBase create(KnowledgeBase kb, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        kb.setTenantId(tenantId);
        kb.setCreatedBy(getUserId(request));
        kb.setStatus("ACTIVE");
        kb.setDocumentCount(0);
        kb.setTotalChunks(0);
        return knowledgeBaseRepository.save(kb);
    }

    public KnowledgeBase getById(Long id, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        return knowledgeBaseRepository.findByIdAndDeletedFalse(id)
                .filter(kb -> kb.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ResponseCode.KNOWLEDGE_BASE_NOT_FOUND));
    }

    public PageResult<KnowledgeBase> list(int page, int pageSize, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        Page<KnowledgeBase> result = knowledgeBaseRepository.findAllByTenantIdAndDeletedFalse(
                tenantId, PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(result.getContent(), result.getTotalElements(), page, pageSize);
    }

    public List<KnowledgeBase> listByOwner(Long ownerId, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        return knowledgeBaseRepository.findAllByTenantIdAndOwnerIdAndDeletedFalse(tenantId, ownerId);
    }

    @Transactional
    public KnowledgeBase update(Long id, KnowledgeBase updates, HttpServletRequest request) {
        KnowledgeBase existing = getById(id, request);
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getEmbeddingModel() != null) existing.setEmbeddingModel(updates.getEmbeddingModel());
        if (updates.getVectorDbType() != null) existing.setVectorDbType(updates.getVectorDbType());
        existing.setUpdatedBy(getUserId(request));
        return knowledgeBaseRepository.save(existing);
    }

    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        KnowledgeBase existing = getById(id, request);
        existing.setDeleted(true);
        existing.setUpdatedBy(getUserId(request));
        knowledgeBaseRepository.save(existing);
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
