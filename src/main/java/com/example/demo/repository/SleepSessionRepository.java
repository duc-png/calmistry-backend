package com.example.demo.repository;

import com.example.demo.entity.SleepSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SleepSessionRepository extends JpaRepository<SleepSession, Long> {

    // Find all sessions for a user, ordered by date descending
    List<SleepSession> findByUserIdOrderByRecordDateDesc(Long userId);

    // Find sessions with pagination
    Page<SleepSession> findByUserIdOrderByRecordDateDesc(Long userId, Pageable pageable);

    // Find latest session for a user
    Optional<SleepSession> findTopByUserIdOrderByRecordDateDesc(Long userId);

    // Find sessions in a date range
    List<SleepSession> findByUserIdAndRecordDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // Check if session exists for user on specific date
    boolean existsByUserIdAndRecordDate(Long userId, LocalDate recordDate);
}
