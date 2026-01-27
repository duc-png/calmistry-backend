package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StoryResponse {
    Long id;
    String author; // "Ẩn danh" if anonymous
    String avatar; // Icon class or URL
    String content;
    String time; // Formatted time string (e.g., "2 giờ trước")
    Integer hearts;
    Boolean isLiked;
    Boolean isAnonymous;
}
