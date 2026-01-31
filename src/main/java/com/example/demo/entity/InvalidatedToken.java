package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Table(name = "invalidated_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvalidatedToken {

    @Id
    @Column(length = 255)
    private String id; // JTI hoặc token string

    @Column(name = "expiry_time", nullable = false)
    private Date expiryTime;
}
