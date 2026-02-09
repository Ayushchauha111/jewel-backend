package com.example.jewell.repository;

import com.example.jewell.model.Layaway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LayawayRepository extends JpaRepository<Layaway, Long> {
    List<Layaway> findByCustomerId(Long customerId);
    List<Layaway> findByStatus(Layaway.LayawayStatus status);
}
