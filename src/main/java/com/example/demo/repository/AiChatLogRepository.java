package com.example.demo.repository;

import com.example.demo.entity.AiChatLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiChatLogRepository extends JpaRepository<AiChatLog, Long> {

    // Get all chat logs for a user, ordered by date descending
    List<AiChatLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Get paginated chat logs
    Page<AiChatLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Get recent conversations after a specific date
    List<AiChatLog> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    // Count total messages for a user
    long countByUserId(Long userId);

    // Delete all chat logs for a user
    void deleteByUserId(Long userId);
}
