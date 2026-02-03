package com.example.demo.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserOnboardingRequest {
    String gender;
    LocalDate dateOfBirth;
    List<String> hobbies;
    String mainGoal;
    String preferredTone;
}
