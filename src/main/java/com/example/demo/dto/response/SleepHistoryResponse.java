package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SleepHistoryResponse {
    List<SleepHistoryItem> sessions;
    Integer totalSessions;
    Double averageScore;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SleepHistoryItem {
        Long sessionId;
        LocalDate recordDate;
        Integer finalScore100;
        String status;
        String categoryTitle;
    }
}
