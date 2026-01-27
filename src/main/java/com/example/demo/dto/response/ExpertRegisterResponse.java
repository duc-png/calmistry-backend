package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpertRegisterResponse {
    Long userId;
    Long expertProfileId;
    String username;
    String email;
    String fullName;
    String specialty;
    String message;
}

