package com.example.demo.repository;

import com.example.demo.entity.SleepScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SleepScoreRepository extends JpaRepository<SleepScore, Long> {

    // Find score by session ID
    Optional<SleepScore> findBySessionId(Long sessionId);
}
