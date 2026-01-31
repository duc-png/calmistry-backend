package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogCreateRequest {
    @NotNull(message = "Category ID is required")
    Long categoryId;

    @NotBlank(message = "Title is required")
    String title;

    String slug; // Optional, sẽ tự generate nếu không có

    @NotBlank(message = "Content is required")
    String content;

    String thumbnailUrl;

    BlogStatus status;

    public enum BlogStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }
}
