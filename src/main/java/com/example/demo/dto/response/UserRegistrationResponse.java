package com.example.demo.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationResponse {

    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String message;
}
