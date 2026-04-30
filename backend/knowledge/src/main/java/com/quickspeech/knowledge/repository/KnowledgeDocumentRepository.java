package com.quickspeech.knowledge.repository;

import com.quickspeech.knowledge.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    Optional<KnowledgeDocument> findByIdAndDeletedFalse(Long id);

    Page<KnowledgeDocument> findAllByTenantIdAndKnowledgeBaseIdAndDeletedFalse(
            Long tenantId, Long knowledgeBaseId, Pageable pageable);

    List<KnowledgeDocument> findAllByTenantIdAndKnowledgeBaseIdAndDeletedFalse(Long tenantId, Long knowledgeBaseId);

    List<KnowledgeDocument> findAllByTenantIdAndStatusAndDeletedFalse(Long tenantId, String status);

    long countByTenantIdAndKnowledgeBaseIdAndDeletedFalse(Long tenantId, Long knowledgeBaseId);
}
