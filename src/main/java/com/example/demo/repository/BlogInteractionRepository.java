package com.example.demo.repository;

import com.example.demo.entity.Blog;
import com.example.demo.entity.BlogInteraction;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogInteractionRepository extends JpaRepository<BlogInteraction, Long> {
    Optional<BlogInteraction> findByUserAndBlogAndInteractionType(User user, Blog blog,
            BlogInteraction.InteractionType type);

    long countByBlogAndInteractionType(Blog blog, BlogInteraction.InteractionType type);

    boolean existsByUserAndBlogAndInteractionType(User user, Blog blog, BlogInteraction.InteractionType type);
}
