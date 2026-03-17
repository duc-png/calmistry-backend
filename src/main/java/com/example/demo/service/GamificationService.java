package com.example.demo.service;

import com.example.demo.dto.response.GamificationSpinBalanceResponse;
import com.example.demo.dto.response.GamificationSpinResultResponse;
import com.example.demo.dto.response.GamificationTodayResponse;
import com.example.demo.entity.GamificationEventType;
import com.example.demo.entity.GamificationSpinEvent;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStats;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.GamificationSpinEventRepository;
import com.example.demo.repository.UserStatsRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GamificationService {
    static final List<String> SYMBOLS = List.of(
            "calmistry_white_logo",
            "banana",
            "seven",
            "cherry",
            "plum",
            "orange",
            "bell",
            "lemon",
            "melon");

    GamificationSpinEventRepository spinEventRepository;
    UserStatsRepository userStatsRepository;
    com.example.demo.repository.UserVoucherRepository userVoucherRepository;
    com.example.demo.repository.VoucherRepository voucherRepository;
    SimpMessagingTemplate messagingTemplate;

    SecureRandom secureRandom = new SecureRandom();

    private void publishRealtimeUpdate(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        try {
            messagingTemplate.convertAndSend("/topic/gamification." + user.getId(), getToday(user));
        } catch (Exception ignored) {
            // best-effort realtime update
        }
    }

    @Transactional
    public boolean awardDailySpin(User user, GamificationEventType eventType) {
        LocalDate today = LocalDate.now();

        boolean alreadyAwarded = spinEventRepository.existsByUser_IdAndEventTypeAndEventDate(user.getId(), eventType,
                today);
        if (alreadyAwarded) {
            return false;
        }

        try {
            GamificationSpinEvent event = GamificationSpinEvent.builder()
                    .user(user)
                    .eventType(eventType)
                    .eventDate(today)
                    .build();
            spinEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            // Unique constraint hit due to race condition; treat as already awarded
            return false;
        }

        UserStats stats = userStatsRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    UserStats s = new UserStats();
                    s.setUser(user);
                    s.setTotalPoints(0);
                    s.setCurrentStreak(0);
                    s.setSpinBalance(0);
                    return s;
                });

        int current = stats.getSpinBalance() == null ? 0 : stats.getSpinBalance();
        stats.setSpinBalance(current + 1);
        userStatsRepository.save(stats);
        publishRealtimeUpdate(user);
        return true;
    }

    @Transactional(readOnly = true)
    public GamificationSpinBalanceResponse getBalance(User user) {
        int balance = userStatsRepository.findByUser_Id(user.getId())
                .map(s -> s.getSpinBalance() == null ? 0 : s.getSpinBalance())
                .orElse(0);
        return GamificationSpinBalanceResponse.builder().spinBalance(balance).build();
    }

    @Transactional(readOnly = true)
    public GamificationTodayResponse getToday(User user) {
        int balance = userStatsRepository.findByUser_Id(user.getId())
                .map(s -> s.getSpinBalance() == null ? 0 : s.getSpinBalance())
                .orElse(0);

        LocalDate today = LocalDate.now();
        List<String> completed = spinEventRepository.findAllByUser_IdAndEventDate(user.getId(), today)
                .stream()
                .map(e -> e.getEventType().name())
                .distinct()
                .collect(Collectors.toList());

        return GamificationTodayResponse.builder()
                .spinBalance(balance)
                .completedEvents(completed)
                .build();
    }

    @Transactional
    public GamificationSpinResultResponse spin(User user) {
        UserStats stats = userStatsRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    UserStats s = new UserStats();
                    s.setUser(user);
                    s.setTotalPoints(0);
                    s.setCurrentStreak(0);
                    s.setSpinBalance(0);
                    return userStatsRepository.save(s);
                });

        int balance = stats.getSpinBalance() == null ? 0 : stats.getSpinBalance();
        if (balance <= 0) {
            throw new AppException(ErrorCode.NO_SPINS_LEFT);
        }

        stats.setSpinBalance(balance - 1);
        userStatsRepository.save(stats);

        double rand = secureRandom.nextDouble();
        int a, b, c;
        boolean jackpot = false;
        String voucherCode = null;
        String voucherTitle = null;

        if (rand < 0.05) {
            // 5% Logo Jackpot (3 Calmistry Logos = index 0)
            a = b = c = 0;
            jackpot = true;
            com.example.demo.entity.Voucher voucher = voucherRepository.findByTitle("Voucher 20%").orElseGet(() -> {
                com.example.demo.entity.Voucher v = com.example.demo.entity.Voucher.builder()
                        .title("Voucher 20%")
                        .discountValue(20.0)
                        .discountType(com.example.demo.entity.Voucher.DiscountType.PERCENTAGE)
                        .applicableTo("ALL")
                        .build();
                return voucherRepository.save(v);
            });
            com.example.demo.entity.UserVoucher uv = com.example.demo.entity.UserVoucher.builder()
                    .user(user)
                    .voucher(voucher)
                    .code("CALM20-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .status(com.example.demo.entity.UserVoucher.VoucherStatus.UNUSED)
                    .expiryDate(java.time.LocalDateTime.now().plusDays(30))
                    .build();
            userVoucherRepository.save(uv);
            voucherCode = uv.getCode();
            voucherTitle = voucher.getTitle();
        } else if (rand < 0.15) {
            // 10% Normal Jackpot (3 matching, skip Logo)
            int item = 1 + secureRandom.nextInt(SYMBOLS.size() - 1);
            a = b = c = item;
            jackpot = true;
            com.example.demo.entity.Voucher voucher = voucherRepository.findByTitle("Voucher 10%").orElseGet(() -> {
                com.example.demo.entity.Voucher v = com.example.demo.entity.Voucher.builder()
                        .title("Voucher 10%")
                        .discountValue(10.0)
                        .discountType(com.example.demo.entity.Voucher.DiscountType.PERCENTAGE)
                        .applicableTo("ALL")
                        .build();
                return voucherRepository.save(v);
            });
            com.example.demo.entity.UserVoucher uv = com.example.demo.entity.UserVoucher.builder()
                    .user(user)
                    .voucher(voucher)
                    .code("CALM10-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .status(com.example.demo.entity.UserVoucher.VoucherStatus.UNUSED)
                    .expiryDate(java.time.LocalDateTime.now().plusDays(30))
                    .build();
            userVoucherRepository.save(uv);
            voucherCode = uv.getCode();
            voucherTitle = voucher.getTitle();
        } else if (rand < 0.60) {
            // 45% Pairs
            int matchItem = secureRandom.nextInt(SYMBOLS.size());
            int otherItem = secureRandom.nextInt(SYMBOLS.size());
            while (otherItem == matchItem) otherItem = secureRandom.nextInt(SYMBOLS.size());

            int pos = secureRandom.nextInt(3);
            if (pos == 0) { a = b = matchItem; c = otherItem; }
            else if (pos == 1) { a = c = matchItem; b = otherItem; }
            else { b = c = matchItem; a = otherItem; }
        } else {
            // 40% Lose (All different)
            a = secureRandom.nextInt(SYMBOLS.size());
            b = secureRandom.nextInt(SYMBOLS.size());
            while (b == a) b = secureRandom.nextInt(SYMBOLS.size());
            c = secureRandom.nextInt(SYMBOLS.size());
            while (c == a || c == b) c = secureRandom.nextInt(SYMBOLS.size());
        }

        GamificationSpinResultResponse response = GamificationSpinResultResponse.builder()
                .symbols(java.util.List.of(SYMBOLS.get(a), SYMBOLS.get(b), SYMBOLS.get(c)))
                .jackpot(jackpot)
                .remainingSpins(stats.getSpinBalance())
                .voucherCode(voucherCode)
                .voucherTitle(voucherTitle)
                .build();
        publishRealtimeUpdate(user);
        return response;
    }
}
