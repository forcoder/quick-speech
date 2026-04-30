package com.quickspeech.common.repository;

import com.quickspeech.common.entity.UserKnowledgeCorrection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserKnowledgeCorrectionRepository extends JpaRepository<UserKnowledgeCorrection, Long> {

    Page<UserKnowledgeCorrection> findAllByTenantIdAndDeletedFalse(Long tenantId, Pageable pageable);

    List<UserKnowledgeCorrection> findAllByTenantIdAndUserIdAndDeletedFalse(Long tenantId, Long userId);

    List<UserKnowledgeCorrection> findAllByTenantIdAndKnowledgeBaseIdAndDeletedFalse(Long tenantId, Long knowledgeBaseId);

    List<UserKnowledgeCorrection> findAllByTenantIdAndStatusAndDeletedFalse(Long tenantId, String status);
}
