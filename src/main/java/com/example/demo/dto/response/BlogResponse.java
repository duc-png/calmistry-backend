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
    String expertName;
    Long categoryId;
    String categoryName;
    String title;
    String slug;
    String content;
    String thumbnailUrl;
    Integer viewCount;
    Blog.BlogStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

