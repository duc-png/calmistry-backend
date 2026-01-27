package com.example.demo.repository;

import com.example.demo.entity.Story;
import com.example.demo.entity.StoryInteraction;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoryInteractionRepository extends JpaRepository<StoryInteraction, Long> {
    Optional<StoryInteraction> findByStoryAndUserAndType(Story story, User user, StoryInteraction.InteractionType type);

    boolean existsByStoryAndUserAndType(Story story, User user, StoryInteraction.InteractionType type);
}
