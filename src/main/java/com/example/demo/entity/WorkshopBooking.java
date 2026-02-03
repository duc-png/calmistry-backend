package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workshop_bookings", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "workshop_id" }))
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class WorkshopBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @Column(name = "booked_at")
    private LocalDateTime bookedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.CONFIRMED;

    @PrePersist
    protected void onBook() {
        bookedAt = LocalDateTime.now();
    }

    public enum BookingStatus {
        CONFIRMED, CANCELLED
    }
}
