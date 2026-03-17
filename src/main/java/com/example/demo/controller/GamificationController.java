package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.response.GamificationSpinBalanceResponse;
import com.example.demo.dto.response.GamificationSpinResultResponse;
import com.example.demo.dto.response.GamificationTodayResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.GamificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gamification")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GamificationController {
    GamificationService gamificationService;
    UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @GetMapping("/spins")
    public ApiResponse<GamificationSpinBalanceResponse> getSpinBalance() {
        User user = getCurrentUser();
        return ApiResponse.<GamificationSpinBalanceResponse>builder()
                .result(gamificationService.getBalance(user))
                .build();
    }

    @PostMapping("/spin")
    public ApiResponse<GamificationSpinResultResponse> spin() {
        User user = getCurrentUser();
        return ApiResponse.<GamificationSpinResultResponse>builder()
                .result(gamificationService.spin(user))
                .build();
    }

    @GetMapping("/today")
    public ApiResponse<GamificationTodayResponse> today() {
        User user = getCurrentUser();
        return ApiResponse.<GamificationTodayResponse>builder()
                .result(gamificationService.getToday(user))
                .build();
    }
}
