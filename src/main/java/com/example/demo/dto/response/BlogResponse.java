package com.example.demo.dto.response;

import com.example.demo.entity.Blog;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogResponse {
    Long id;
    Long expertId;
    String expertName; // Keep for backward compatibility
    String authorName;
    String authorAvatar;
    Long categoryId;
    String categoryName;
    String title;
    String slug;
    String content;
    String thumbnailUrl;
    Integer viewCount;
    Long likeCount;
    Long commentCount;
    boolean isLiked;
    Blog.BlogStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
