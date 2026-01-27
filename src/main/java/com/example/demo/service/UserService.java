package com.example.demo.service;

import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStats;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserStatsRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    UserStatsRepository userStatsRepository;

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Get or Create Stats
        UserStats stats = userStatsRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    UserStats newStats = new UserStats();
                    newStats.setUser(user);
                    newStats.setTotalPoints(0);
                    newStats.setCurrentStreak(0);
                    return userStatsRepository.save(newStats);
                });

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .fuedScore(stats.getTotalPoints())
                .currentStreak(stats.getCurrentStreak())
                .lastActivityDate(stats.getLastActivityDate())
                .build();
    }
}
