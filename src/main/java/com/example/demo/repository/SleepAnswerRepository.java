package com.example.demo.repository;

import com.example.demo.entity.SleepAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SleepAnswerRepository extends JpaRepository<SleepAnswer, Long> {

    // Find all answers for a specific session
    List<SleepAnswer> findBySessionId(Long sessionId);
}
