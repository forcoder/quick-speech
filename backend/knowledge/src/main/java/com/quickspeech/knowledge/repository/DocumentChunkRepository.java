package com.quickspeech.knowledge.repository;

import com.quickspeech.knowledge.entity.DocumentChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findAllByTenantIdAndDocumentIdAndDeletedFalseOrderByChunkIndexAsc(Long tenantId, Long documentId);

    Page<DocumentChunk> findAllByTenantIdAndKnowledgeBaseIdAndDeletedFalse(
            Long tenantId, Long knowledgeBaseId, Pageable pageable);

    List<DocumentChunk> findAllByTenantIdAndKnowledgeBaseIdAndIsVectorizedFalseAndDeletedFalse(
            Long tenantId, Long knowledgeBaseId);

    @Modifying
    @Query("UPDATE DocumentChunk d SET d.deleted = true WHERE d.tenantId = :tenantId AND d.documentId = :documentId AND d.deleted = false")
    int softDeleteByDocumentId(@Param("tenantId") Long tenantId, @Param("documentId") Long documentId);

    long countByTenantIdAndKnowledgeBaseIdAndDeletedFalse(Long tenantId, Long knowledgeBaseId);
}
