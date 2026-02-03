package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    Long id;
    String username;
    String fullName;
    String email;
    Integer fuedScore; // Maps to totalPoints in UserStats
    Integer currentStreak;
    LocalDate lastActivityDate;
    java.util.Set<String> roles;
    Boolean isOnboarded;
    String gender;
    LocalDate dateOfBirth;
    String hobbies;
    String mainGoal;
    String preferredTone;
}
