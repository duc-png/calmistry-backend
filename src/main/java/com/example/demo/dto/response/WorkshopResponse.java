package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkshopResponse {
    Long id;
    String title;
    String description;
    LocalDateTime startTime;
    LocalDateTime endTime;
    String speakerName;
    String speakerBio;
    Integer maxParticipants;
    Integer currentParticipants;
    String status;
    String imageUrl;
    String location;
}
