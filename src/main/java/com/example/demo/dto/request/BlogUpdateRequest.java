package com.example.demo.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogUpdateRequest {
    String title;
    String content;
    String slug;
    Long categoryId;

    @Builder.Default
    @Size(max = 20, message = "Maximum 20 images allowed")
    List<String> imageUrls = new ArrayList<>();

    String status; // PUBLISHED, PENDING, etc.
}
