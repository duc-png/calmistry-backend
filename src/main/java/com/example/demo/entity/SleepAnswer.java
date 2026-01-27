package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sleep_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SleepAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private SleepSession session;

    @Column(name = "question_code", length = 20)
    private String questionCode;

    @Column(name = "answer_value", columnDefinition = "TEXT")
    private String answerValue;
}

