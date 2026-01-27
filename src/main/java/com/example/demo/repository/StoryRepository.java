package com.example.demo.repository;

import com.example.demo.entity.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    Page<Story> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // For counting user's stories if needed for scoring
    long countByUserId(Long userId);
}
