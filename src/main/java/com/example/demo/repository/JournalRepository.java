package com.example.demo.repository;

import com.example.demo.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Long> {

    // Find all journals for a user, ordered by date descending
    List<Journal> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Find journals by user and mood
    List<Journal> findByUserIdAndMoodOrderByCreatedAtDesc(Long userId, String mood);

    // Search journals by title or content
    List<Journal> findByUserIdAndTitleContainingOrUserIdAndContentContainingOrderByCreatedAtDesc(
            Long userId1, String titleKeyword, Long userId2, String contentKeyword);

    // Find single journal by id and user (for security check)
    Optional<Journal> findByIdAndUserId(Long id, Long userId);
}
