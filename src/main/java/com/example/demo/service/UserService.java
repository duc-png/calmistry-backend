package com.example.demo.service;

import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStats;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserStatsRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    UserStatsRepository userStatsRepository;
    RoleRepository roleRepository;

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

        return mapToResponse(user);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        com.example.demo.entity.Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        user.setRoles(new HashSet<>(Set.of(role)));

        return mapToResponse(userRepository.save(user));
    }

    private UserResponse mapToResponse(User user) {
        // Need to fetch stats or use default since findAll doesn't fetch stats by
        // default lazy load
        int points = 0;
        int streak = 0;
        java.time.LocalDate lastActivity = null;

        if (user.getUserStats() != null) {
            points = user.getUserStats().getTotalPoints();
            streak = user.getUserStats().getCurrentStreak();
            lastActivity = user.getUserStats().getLastActivityDate();
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .fuedScore(points)
                .currentStreak(streak)
                .lastActivityDate(lastActivity)
                .roles(user.getRoles().stream().map(com.example.demo.entity.Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
}
