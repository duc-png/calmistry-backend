package com.example.demo.service;

import com.example.demo.dto.request.SubmitFuiedsRequest;
import com.example.demo.dto.response.FuiedsScoreResponse;
import com.example.demo.entity.FuiedsResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.FuiedsResponseRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FuiedsService {

    private final FuiedsResponseRepository fuiedsResponseRepository;
    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        log.info("✅ FuiedsService initialized - Mental well-being scoring system ready");
    }

    // Weights for each component (must sum to 1.0)
    private static final double FEELINGS_WEIGHT = 0.20;
    private static final double UNDERSTANDING_WEIGHT = 0.15;
    private static final double INTERACTION_WEIGHT = 0.15;
    private static final double ENERGY_WEIGHT = 0.20;
    private static final double DRIVE_WEIGHT = 0.15;
    private static final double STABILITY_WEIGHT = 0.15;

    // EMA smoothing parameters
    private static final double TODAY_WEIGHT = 0.7;
    private static final double HISTORY_WEIGHT = 0.3;

    /**
     * Submit daily FUIEDS quiz response
     */
    @Transactional
    public FuiedsScoreResponse submitResponse(SubmitFuiedsRequest request) {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now();

        // Check if already submitted today
        fuiedsResponseRepository.findByUserAndResponseDate(user, today)
                .ifPresent(existing -> {
                    throw new RuntimeException("Bạn đã hoàn thành đánh giá hôm nay rồi!");
                });

        // Normalize answers to 0-100 scale
        Double feelingsScore = normalizeScore(request.getFeelingsAnswer());
        Double understandingScore = normalizeScore(request.getUnderstandingAnswer());
        Double interactionScore = normalizeScore(request.getInteractionAnswer());
        Double energyScore = normalizeScore(request.getEnergyAnswer());
        Double driveScore = normalizeScore(request.getDriveAnswer());
        Double stabilityScore = normalizeScore(request.getStabilityAnswer());

        // Calculate weighted total score
        Double rawScore = calculateWeightedScore(
                feelingsScore, understandingScore, interactionScore,
                energyScore, driveScore, stabilityScore);

        // Calculate smoothed score using EMA
        Double smoothedScore = calculateSmoothedScore(user, rawScore, today);

        // Determine "good enough" status
        Boolean isGoodEnough = isGoodEnough(
                smoothedScore,
                feelingsScore, understandingScore, interactionScore,
                energyScore, driveScore, stabilityScore);

        // Save response
        FuiedsResponse response = FuiedsResponse.builder()
                .user(user)
                .responseDate(today)
                .feelingsScore(feelingsScore)
                .understandingScore(understandingScore)
                .interactionScore(interactionScore)
                .energyScore(energyScore)
                .driveScore(driveScore)
                .stabilityScore(stabilityScore)
                .rawTotalScore(rawScore)
                .smoothedScore(smoothedScore)
                .isGoodEnough(isGoodEnough)
                .build();

        response = fuiedsResponseRepository.save(response);

        log.info("FUIEDS response submitted for user {}: score={}, smoothed={}, goodEnough={}",
                user.getUsername(), rawScore, smoothedScore, isGoodEnough);

        return toResponse(response);
    }

    /**
     * Get today's score
     */
    public FuiedsScoreResponse getTodayScore() {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now();

        return fuiedsResponseRepository.findByUserAndResponseDate(user, today)
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * Get score history for last N days
     */
    public List<FuiedsScoreResponse> getHistory(int days) {
        User user = getCurrentUser();
        LocalDate startDate = LocalDate.now().minusDays(days);

        return fuiedsResponseRepository.findByUserAndResponseDateAfter(user, startDate)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Normalize answer (0-4) to score (0-100)
     * Formula: (answer / 4) * 100
     */
    private Double normalizeScore(Integer answer) {
        if (answer == null || answer < 0 || answer > 4) {
            throw new IllegalArgumentException("Answer must be between 0 and 4");
        }
        return (answer / 4.0) * 100.0;
    }

    /**
     * Calculate weighted total score
     * FUIEDS = 0.20×F + 0.15×U + 0.15×I + 0.20×E + 0.15×D + 0.15×S
     */
    private Double calculateWeightedScore(
            Double f, Double u, Double i, Double e, Double d, Double s) {
        return FEELINGS_WEIGHT * f +
                UNDERSTANDING_WEIGHT * u +
                INTERACTION_WEIGHT * i +
                ENERGY_WEIGHT * e +
                DRIVE_WEIGHT * d +
                STABILITY_WEIGHT * s;
    }

    /**
     * Calculate smoothed score using Exponential Moving Average (EMA)
     * Formula: 0.7 × TodayScore + 0.3 × Average(3 days before)
     */
    private Double calculateSmoothedScore(User user, Double todayScore, LocalDate today) {
        // Get last 3 scores before today
        List<FuiedsResponse> recentResponses = fuiedsResponseRepository
                .findTop3ByUserAndBeforeDate(user, today);

        if (recentResponses.isEmpty()) {
            // First time - no smoothing
            return todayScore;
        }

        // Calculate average of recent scores (up to 3 days)
        double avgRecent = recentResponses.stream()
                .limit(3)
                .mapToDouble(FuiedsResponse::getRawTotalScore)
                .average()
                .orElse(todayScore);

        return TODAY_WEIGHT * todayScore + HISTORY_WEIGHT * avgRecent;
    }

    /**
     * Determine if response meets "good enough" criteria
     * Criteria: Score >= 70 AND at least 5 out of 6 components >= 60
     */
    private Boolean isGoodEnough(
            Double totalScore,
            Double f, Double u, Double i, Double e, Double d, Double s) {
        if (totalScore < 70) {
            return false;
        }

        long componentsAbove60 = List.of(f, u, i, e, d, s)
                .stream()
                .filter(score -> score >= 60)
                .count();

        return componentsAbove60 >= 5;
    }

    /**
     * Convert entity to response DTO
     */
    private FuiedsScoreResponse toResponse(FuiedsResponse entity) {
        String status = getStatusText(entity.getSmoothedScore());
        String color = getStatusColor(entity.getSmoothedScore());

        return FuiedsScoreResponse.builder()
                .id(entity.getId())
                .date(entity.getResponseDate())
                .feelingsScore(entity.getFeelingsScore())
                .understandingScore(entity.getUnderstandingScore())
                .interactionScore(entity.getInteractionScore())
                .energyScore(entity.getEnergyScore())
                .driveScore(entity.getDriveScore())
                .stabilityScore(entity.getStabilityScore())
                .rawScore(entity.getRawTotalScore())
                .smoothedScore(entity.getSmoothedScore())
                .isGoodEnough(entity.getIsGoodEnough())
                .status(status)
                .statusColor(color)
                .build();
    }

    /**
     * Get status text based on score
     */
    private String getStatusText(Double score) {
        if (score >= 80)
            return "Rất tốt";
        if (score >= 60)
            return "Tốt";
        if (score >= 40)
            return "Tạm ổn";
        return "Nguy cơ cao";
    }

    /**
     * Get status color based on score
     */
    private String getStatusColor(Double score) {
        if (score >= 80)
            return "#28a745"; // Dark green
        if (score >= 60)
            return "#74c655"; // Light green
        if (score >= 40)
            return "#ffc107"; // Yellow
        return "#dc3545"; // Red
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
