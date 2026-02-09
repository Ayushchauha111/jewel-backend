package com.example.jewell.repository;

import com.example.jewell.model.DaySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DaySessionRepository extends JpaRepository<DaySession, Long> {
    Optional<DaySession> findBySessionDate(LocalDate sessionDate);
    Optional<DaySession> findBySessionDateAndStatus(LocalDate sessionDate, DaySession.SessionStatus status);
}
