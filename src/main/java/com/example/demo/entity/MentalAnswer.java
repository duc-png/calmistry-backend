package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mental_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MentalAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private MentalSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension")
    private MentalDimension dimension;

    @Column(name = "value")
    private Integer value;

    public enum MentalDimension {
        F, U, I, E, D, S
    }
}

