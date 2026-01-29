package com.example.jewell.repository;

import com.example.jewell.model.DigitalCitizenshipLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DigitalCitizenshipLessonRepository extends JpaRepository<DigitalCitizenshipLesson, Long> {
    List<DigitalCitizenshipLesson> findByIsActiveTrueOrderByDisplayOrderAsc();
}
