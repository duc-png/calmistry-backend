package com.example.demo.service;

import com.example.demo.dto.request.AnswerDTO;
import com.example.demo.dto.request.SubmitSleepQuizRequest;
import com.example.demo.dto.response.SleepHistoryResponse;
import com.example.demo.dto.response.SleepSessionResponse;
import com.example.demo.entity.*;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.SleepAnswerRepository;
import com.example.demo.repository.SleepScoreRepository;
import com.example.demo.repository.SleepSessionRepository;
import com.example.demo.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SleepService {

    SleepSessionRepository sleepSessionRepository;
    SleepAnswerRepository sleepAnswerRepository;
    SleepScoreRepository sleepScoreRepository;
    UserRepository userRepository;

    /**
     * Submit sleep quiz and calculate score
     */
    @Transactional
    public SleepSessionResponse submitSleepQuiz(SubmitSleepQuizRequest request) {
        // Get authenticated user
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Check if session already exists for this date
        if (sleepSessionRepository.existsByUserIdAndRecordDate(user.getId(), request.getRecordDate())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // You may want to create a specific error code
        }

        // Create session
        SleepSession session = new SleepSession();
        session.setUser(user);
        session.setRecordDate(request.getRecordDate());
        session = sleepSessionRepository.save(session);

        // Save answers
        for (AnswerDTO answerDTO : request.getAnswers()) {
            SleepAnswer answer = new SleepAnswer();
            answer.setSession(session);
            answer.setQuestionCode(answerDTO.getQuestionCode());
            answer.setAnswerValue(answerDTO.getAnswerValue());
            sleepAnswerRepository.save(answer);
        }

        // Calculate and save score
        SleepScore score = calculateScore(session, request.getAnswers());
        score.setSession(session);
        score = sleepScoreRepository.save(score);

        // Build response
        return buildSessionResponse(session, score, request.getAnswers());
    }

    /**
     * Get user's sleep history
     */
    public SleepHistoryResponse getSleepHistory(int page, int size) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Pageable pageable = PageRequest.of(page, size);
        Page<SleepSession> sessionsPage = sleepSessionRepository.findByUserIdOrderByRecordDateDesc(user.getId(),
                pageable);

        List<SleepHistoryResponse.SleepHistoryItem> items = sessionsPage.getContent().stream()
                .map(session -> {
                    SleepScore score = sleepScoreRepository.findBySessionId(session.getId()).orElse(null);
                    return SleepHistoryResponse.SleepHistoryItem.builder()
                            .sessionId(session.getId())
                            .recordDate(session.getRecordDate())
                            .finalScore100(score != null ? score.getFinalScore100() : 0)
                            .status(score != null && score.getStatus() != null ? score.getStatus().name() : "UNKNOWN")
                            .build();
                })
                .collect(Collectors.toList());

        // Calculate average score
        double averageScore = items.stream()
                .mapToInt(SleepHistoryResponse.SleepHistoryItem::getFinalScore100)
                .average()
                .orElse(0.0);

        return SleepHistoryResponse.builder()
                .sessions(items)
                .totalSessions((int) sessionsPage.getTotalElements())
                .averageScore(averageScore)
                .build();
    }

    /**
     * Get latest sleep session
     */
    public SleepSessionResponse getLatestSleepSession() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        SleepSession session = sleepSessionRepository.findTopByUserIdOrderByRecordDateDesc(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // No sessions found

        SleepScore score = sleepScoreRepository.findBySessionId(session.getId()).orElse(null);
        List<SleepAnswer> answers = sleepAnswerRepository.findBySessionId(session.getId());

        List<AnswerDTO> answerDTOs = answers.stream()
                .map(a -> new AnswerDTO(a.getQuestionCode(), a.getAnswerValue()))
                .collect(Collectors.toList());

        return buildSessionResponse(session, score, answerDTOs);
    }

    /**
     * Calculate sleep score based on answers
     * Uses a simplified PSQI-inspired algorithm
     */
    private SleepScore calculateScore(SleepSession session, List<AnswerDTO> answers) {
        SleepScore score = new SleepScore();

        // Simple scoring: sum all answer values (assuming they are point values)
        int totalPoints = 0;
        for (AnswerDTO answer : answers) {
            try {
                totalPoints += Integer.parseInt(answer.getAnswerValue());
            } catch (NumberFormatException e) {
                log.warn("Could not parse answer value as integer: {}", answer.getAnswerValue());
            }
        }

        // Normalize to 0-100 scale (assuming max possible is 100)
        int finalScore = Math.min(100, totalPoints);

        // Calculate PSQI score (inverse relationship - lower is better in PSQI)
        // For simplicity, we'll use: PSQI = (100 - finalScore) / 5
        int psqiScore = Math.max(0, Math.min(21, (100 - finalScore) / 5));

        // Mock sleep efficiency (in real scenario, this would be calculated from sleep
        // duration data)
        BigDecimal sleepEfficiency = BigDecimal.valueOf(finalScore * 0.88)
                .setScale(2, RoundingMode.HALF_UP);

        // Determine status
        SleepScore.SleepStatus status;
        if (finalScore >= 85) {
            status = SleepScore.SleepStatus.EXCELLENT;
        } else if (finalScore >= 70) {
            status = SleepScore.SleepStatus.GOOD;
        } else if (finalScore >= 50) {
            status = SleepScore.SleepStatus.FAIR;
        } else {
            status = SleepScore.SleepStatus.POOR;
        }

        score.setPsqiScore(psqiScore);
        score.setSleepEfficiencyPercent(sleepEfficiency);
        score.setFinalScore100(finalScore);
        score.setStatus(status);

        return score;
    }

    /**
     * Build session response DTO
     */
    private SleepSessionResponse buildSessionResponse(SleepSession session, SleepScore score, List<AnswerDTO> answers) {
        List<SleepSessionResponse.AnswerResponse> answerResponses = answers.stream()
                .map(a -> SleepSessionResponse.AnswerResponse.builder()
                        .questionCode(a.getQuestionCode())
                        .answerValue(a.getAnswerValue())
                        .build())
                .collect(Collectors.toList());

        return SleepSessionResponse.builder()
                .sessionId(session.getId())
                .recordDate(session.getRecordDate())
                .createdAt(session.getCreatedAt())
                .psqiScore(score != null ? score.getPsqiScore() : null)
                .sleepEfficiencyPercent(score != null ? score.getSleepEfficiencyPercent() : null)
                .finalScore100(score != null ? score.getFinalScore100() : 0)
                .status(score != null && score.getStatus() != null ? score.getStatus().name() : "UNKNOWN")
                .answers(answerResponses)
                .build();
    }
}
