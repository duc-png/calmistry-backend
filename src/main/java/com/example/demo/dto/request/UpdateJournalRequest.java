package com.example.demo.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateJournalRequest {

    String title;
    String content;

    @Pattern(regexp = "happy|neutral|sad", message = "Mood must be happy, neutral, or sad")
    String mood;
}
