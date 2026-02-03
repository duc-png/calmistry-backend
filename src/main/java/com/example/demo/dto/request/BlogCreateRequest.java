package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Builder.Default
    @Size(max = 20, message = "Maximum 20 images allowed")
    java.util.List<String> imageUrls = new java.util.ArrayList<>();

    BlogStatus status;

    public enum BlogStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }
}
