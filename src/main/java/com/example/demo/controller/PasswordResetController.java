package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.ForgotPasswordRequest;
import com.example.demo.dto.request.ResetPasswordRequest;
import com.example.demo.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordResetController {
    PasswordResetService passwordResetService;

    @PostMapping("/request-otp")
    public ApiResponse<String> requestOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestOtp(request.getEmail());
        return ApiResponse.<String>builder()
                .result("Nếu email tồn tại, Calmistry đã gửi OTP. Vui lòng kiểm tra hộp thư.")
                .build();
    }

    @PostMapping("/reset")
    public ApiResponse<String> reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ApiResponse.<String>builder()
                .result("Đặt lại mật khẩu thành công.")
                .build();
    }
}

