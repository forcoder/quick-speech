package com.quickspeech.knowledge.repository;

import com.quickspeech.knowledge.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    Optional<KnowledgeBase> findByIdAndDeletedFalse(Long id);

    Page<KnowledgeBase> findAllByTenantIdAndDeletedFalse(Long tenantId, Pageable pageable);

    List<KnowledgeBase> findAllByTenantIdAndOwnerIdAndDeletedFalse(Long tenantId, Long ownerId);

    List<KnowledgeBase> findAllByTenantIdAndStatusAndDeletedFalse(Long tenantId, String status);

    long countByTenantIdAndDeletedFalse(Long tenantId);
}
