package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mental_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MentalScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "session_id", unique = true)
    private MentalSession session;

    @Column(name = "f_score")
    private Integer fScore;

    @Column(name = "u_score")
    private Integer uScore;

    @Column(name = "i_score")
    private Integer iScore;

    @Column(name = "e_score")
    private Integer eScore;

    @Column(name = "d_score")
    private Integer dScore;

    @Column(name = "s_score")
    private Integer sScore;

    @Column(name = "raw_total_score", precision = 5, scale = 2)
    private BigDecimal rawTotalScore;

    @Column(name = "final_score")
    private Integer finalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MentalStatus status;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }

    public enum MentalStatus {
        LOW, NORMAL, GOOD, EXCELLENT
    }
}

