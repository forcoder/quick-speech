package com.quickspeech.agent.repository;

import com.quickspeech.agent.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    List<PromptTemplate> findAllByTenantIdAndAgentIdAndDeletedFalse(Long tenantId, Long agentId);

    Optional<PromptTemplate> findByTenantIdAndAgentIdAndIsDefaultTrueAndDeletedFalse(Long tenantId, Long agentId);

    List<PromptTemplate> findAllByTenantIdAndDeletedFalse(Long tenantId);
}
