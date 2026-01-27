package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mental_recommendations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MentalRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension")
    private MentalAnswer.MentalDimension dimension;

    @Column(name = "min_score")
    private Integer minScore;

    @Column(name = "max_score")
    private Integer maxScore;

    @Column(name = "suggestion_text", length = 255)
    private String suggestionText;
}

