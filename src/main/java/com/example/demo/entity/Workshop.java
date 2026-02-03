package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "workshops")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Workshop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "speaker_name")
    private String speakerName;

    @Column(name = "speaker_bio", columnDefinition = "TEXT")
    private String speakerBio;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "current_participants")
    @Builder.Default
    private Integer currentParticipants = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WorkshopStatus status = WorkshopStatus.UPCOMING;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "location") // Can be "Online" or a physical address
    private String location;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currentParticipants == null)
            currentParticipants = 0;
    }

    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<WorkshopBooking> bookings = new HashSet<>();

    public enum WorkshopStatus {
        UPCOMING, ONGOING, COMPLETED, CANCELLED
    }
}
