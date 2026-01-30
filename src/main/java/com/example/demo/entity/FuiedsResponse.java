package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fuieds_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuiedsResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate responseDate;

    // Component scores (0-100 after normalization)
    @Column(nullable = false)
    private Double feelingsScore;

    @Column(nullable = false)
    private Double understandingScore;

    @Column(nullable = false)
    private Double interactionScore;

    @Column(nullable = false)
    private Double energyScore;

    @Column(nullable = false)
    private Double driveScore;

    @Column(nullable = false)
    private Double stabilityScore;

    // Calculated scores
    @Column(nullable = false)
    private Double rawTotalScore; // Weighted sum without smoothing

    @Column(nullable = false)
    private Double smoothedScore; // EMA smoothed score

    @Column(nullable = false)
    private Boolean isGoodEnough; // Score >= 70 AND 5/6 components >= 60

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (responseDate == null) {
            responseDate = LocalDate.now();
        }
    }
}
