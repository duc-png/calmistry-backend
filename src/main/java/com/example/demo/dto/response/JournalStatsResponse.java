package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JournalStatsResponse {
    long happyCount;
    long neutralCount;
    long sadCount;
    long totalEntries;
    String aiAnalysis;
}
