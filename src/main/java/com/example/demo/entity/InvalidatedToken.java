package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invalidated_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvalidatedToken {

    @Id
    @Column(length = 255)
    private String id; // JTI hoặc token string

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;
}
