package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuiedsScoreResponse {

    private Long id;
    private LocalDate date;

    // Component scores (0-100)
    private Double feelingsScore;
    private Double understandingScore;
    private Double interactionScore;
    private Double energyScore;
    private Double driveScore;
    private Double stabilityScore;

    // Total scores
    private Double rawScore;
    private Double smoothedScore;

    // Status
    private Boolean isGoodEnough;
    private String status; // "Rất tốt", "Tốt", "Tạm ổn", "Nguy cơ"
    private String statusColor; // Color code for UI
}
