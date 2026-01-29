package com.example.jewell.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.jewell.model.Problem;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    List<Problem> findByTagsContainingIgnoreCase(String tag);
}