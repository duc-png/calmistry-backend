package com.example.demo.service;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.PasswordResetOtp;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.PasswordResetOtpRepository;
import com.example.demo.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordResetService {
    static final int OTP_LENGTH = 6;
    static final int OTP_VALID_MINUTES = 10;
    static final int MAX_ATTEMPTS = 5;

    UserRepository userRepository;
    PasswordResetOtpRepository passwordResetOtpRepository;
    PasswordEncoder passwordEncoder;
    EmailService emailService;

    @Transactional
    public void requestOtp(String email) {
        // Always respond as success to avoid email enumeration.
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.info("[ForgotPassword] Email not found (ignored): {}", email);
            return;
        }

        // Invalidate last OTP (best-effort) to avoid multiple valid codes.
        passwordResetOtpRepository.findTopByUserOrderByCreatedAtDesc(user).ifPresent(last -> {
            if (!last.isUsed() && last.getExpiresAt() != null && last.getExpiresAt().isAfter(LocalDateTime.now())) {
                last.setUsedAt(LocalDateTime.now());
                passwordResetOtpRepository.save(last);
            }
        });

        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        PasswordResetOtp record = PasswordResetOtp.builder()
                .user(user)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES))
                .attempts(0)
                .build();
        passwordResetOtpRepository.save(record);

        emailService.sendOtpEmail(email, otp, OTP_VALID_MINUTES);
        log.info("[ForgotPassword] OTP issued for {}", email);
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        PasswordResetOtp record = passwordResetOtpRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "OTP không hợp lệ hoặc đã hết hạn."));

        if (record.isUsed()) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "OTP đã được sử dụng.");
        }
        if (record.getExpiresAt() == null || record.getExpiresAt().isBefore(LocalDateTime.now())) {
            record.setUsedAt(LocalDateTime.now());
            passwordResetOtpRepository.save(record);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "OTP đã hết hạn.");
        }

        if (record.getAttempts() != null && record.getAttempts() >= MAX_ATTEMPTS) {
            record.setUsedAt(LocalDateTime.now());
            passwordResetOtpRepository.save(record);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Bạn đã nhập sai OTP quá nhiều lần. Vui lòng yêu cầu mã mới.");
        }

        boolean ok = passwordEncoder.matches(otp, record.getOtpHash());
        if (!ok) {
            record.setAttempts((record.getAttempts() == null ? 0 : record.getAttempts()) + 1);
            passwordResetOtpRepository.save(record);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "OTP không đúng.");
        }

        if (newPassword.length() < 6) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        record.setUsedAt(LocalDateTime.now());
        passwordResetOtpRepository.save(record);
        log.info("[ForgotPassword] Password reset successful for {}", email);
    }

    private String generateOtp() {
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        int max = (int) Math.pow(10, OTP_LENGTH) - 1;
        int num = ThreadLocalRandom.current().nextInt(min, max + 1);
        return String.valueOf(num);
    }
}

