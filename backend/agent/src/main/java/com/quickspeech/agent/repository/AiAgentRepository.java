package com.quickspeech.agent.repository;

import com.quickspeech.agent.entity.AiAgent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiAgentRepository extends JpaRepository<AiAgent, Long> {

    Optional<AiAgent> findByIdAndDeletedFalse(Long id);

    Page<AiAgent> findAllByTenantIdAndDeletedFalse(Long tenantId, Pageable pageable);

    List<AiAgent> findAllByTenantIdAndOwnerIdAndDeletedFalse(Long tenantId, Long ownerId);

    List<AiAgent> findAllByTenantIdAndScenarioAndDeletedFalse(Long tenantId, String scenario);

    List<AiAgent> findAllByTenantIdAndStatusAndDeletedFalse(Long tenantId, String status);

    long countByTenantIdAndDeletedFalse(Long tenantId);
}
