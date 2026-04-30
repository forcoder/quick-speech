package com.quickspeech.common.repository;

import com.quickspeech.common.entity.UserBehaviorRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserBehaviorRecordRepository extends JpaRepository<UserBehaviorRecord, Long> {

    Page<UserBehaviorRecord> findAllByTenantIdAndUserIdAndDeletedFalse(Long tenantId, Long userId, Pageable pageable);

    List<UserBehaviorRecord> findAllByTenantIdAndUserIdAndCreatedAtBetweenAndDeletedFalse(
            Long tenantId, Long userId, LocalDateTime start, LocalDateTime end);

    List<UserBehaviorRecord> findAllByTenantIdAndUserIdAndActionTypeAndDeletedFalse(
            Long tenantId, Long userId, String actionType);

    long countByTenantIdAndUserIdAndDeletedFalse(Long tenantId, Long userId);
}
