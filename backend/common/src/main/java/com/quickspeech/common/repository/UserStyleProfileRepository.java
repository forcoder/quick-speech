package com.quickspeech.common.repository;

import com.quickspeech.common.entity.UserStyleProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStyleProfileRepository extends JpaRepository<UserStyleProfile, Long> {

    Optional<UserStyleProfile> findByUserIdAndDeletedFalse(Long userId);
}
