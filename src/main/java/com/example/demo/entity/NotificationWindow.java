package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "notification_windows")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationWindow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "time_slot", nullable = false)
    private LocalTime timeSlot;

    @Column(name = "is_active")
    private Boolean isActive = true;
}

