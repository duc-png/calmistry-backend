package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JournalResponse {
    Long id;
    String title;
    String content;
    String mood;
    String aiResponse;
    LocalDateTime createdAt;
}
