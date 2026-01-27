package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SleepSessionResponse {
    Long sessionId;
    LocalDate recordDate;
    LocalDateTime createdAt;

    // Score information
    Integer psqiScore;
    BigDecimal sleepEfficiencyPercent;
    Integer finalScore100;
    String status; // POOR, FAIR, GOOD, EXCELLENT

    // Answers
    List<AnswerResponse> answers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AnswerResponse {
        String questionCode;
        String answerValue;
    }
}
