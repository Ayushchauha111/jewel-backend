package com.example.jewell.repository;

import com.example.jewell.model.AdminSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminSessionRepository extends JpaRepository<AdminSession, Long> {

    long countByUserId(Long userId);

    List<AdminSession> findByUserIdOrderByLastUsedAtAsc(Long userId);

    Optional<AdminSession> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);
}
