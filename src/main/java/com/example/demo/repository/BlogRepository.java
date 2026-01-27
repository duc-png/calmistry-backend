package com.example.demo.repository;

import com.example.demo.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    Optional<Blog> findBySlug(String slug);
    List<Blog> findByExpertId(Long expertId);
    List<Blog> findByStatus(Blog.BlogStatus status);
    List<Blog> findByCategoryId(Long categoryId);
    
    // Search by title (case-insensitive, partial match)
    List<Blog> findByTitleContainingIgnoreCase(String title);
    
    // Search by title and category
    List<Blog> findByTitleContainingIgnoreCaseAndCategoryId(String title, Long categoryId);
    
    // Search by title and status
    List<Blog> findByTitleContainingIgnoreCaseAndStatus(String title, Blog.BlogStatus status);
    
    // Search by title, category and status
    List<Blog> findByTitleContainingIgnoreCaseAndCategoryIdAndStatus(
        String title, Long categoryId, Blog.BlogStatus status
    );
    
    // Search by category name (using JOIN)
    @Query("SELECT b FROM Blog b JOIN b.category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :categoryName, '%'))")
    List<Blog> findByCategoryNameContainingIgnoreCase(@Param("categoryName") String categoryName);
}

