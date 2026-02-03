package com.example.demo.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkshopRequest {
    String title;
    String description;
    LocalDateTime startTime;
    LocalDateTime endTime;
    String speakerName;
    String speakerBio;
    Integer maxParticipants;
    String imageUrl;
    String location;
}
