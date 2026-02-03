package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "sleep_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SleepScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "session_id", unique = true)
    private SleepSession session;

    @Column(name = "psqi_score")
    private Integer psqiScore;

    @Column(name = "sleep_efficiency_percent", precision = 5, scale = 2)
    private BigDecimal sleepEfficiencyPercent;

    @Column(name = "final_score_100")
    private Integer finalScore100;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SleepStatus status;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "category_title")
    private String categoryTitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "advice", columnDefinition = "TEXT")
    private String advice;

    public enum SleepStatus {
        POOR, FAIR, GOOD, EXCELLENT
    }
}
