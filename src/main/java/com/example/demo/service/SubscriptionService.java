package com.example.demo.service;

import com.example.demo.dto.response.GoldCheckoutResponse;
import com.example.demo.entity.SubscriptionOrder;
import com.example.demo.entity.User;
import com.example.demo.entity.UserPlan;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.SubscriptionOrderRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkshopBookingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionService {
    static final int GOLD_PRICE_VND = 2000;

    PayOS payOS;
    UserRepository userRepository;
    SubscriptionOrderRepository subscriptionOrderRepository;
    WorkshopBookingRepository workshopBookingRepository;

    @lombok.experimental.NonFinal
    @Value("${app.frontend-url:http://localhost:5173}")
    String frontendUrl;

    @Transactional
    public GoldCheckoutResponse createGoldCheckout(String voucherCode) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (isGoldEffective(user)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Tài khoản của bạn đã là gói Vàng.");
        }

        long orderCode = generateUniqueOrderCode();
        long expiredAt = Instant.now().getEpochSecond() + (15 * 60);

        // Strip trailing slash from frontendUrl to avoid double-slash
        String baseUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;

        ItemData item = ItemData.builder()
                .name("Goi Vang")
                .price(GOLD_PRICE_VND)
                .quantity(1)
                .build();

        PaymentData paymentData = PaymentData.builder()
                .orderCode(orderCode)
                .amount(GOLD_PRICE_VND)
                .description("Nang cap Goi Vang")
                .returnUrl(baseUrl + "/checkout?plan=gold&status=success")
                .cancelUrl(baseUrl + "/checkout?plan=gold&status=cancel")
                .item(item)
                .expiredAt(expiredAt)
                .build();

        String checkoutUrl;
        try {
            log.info("📦 Creating PayOS GOLD payment link. OrderCode: {}, Amount: {}", orderCode, GOLD_PRICE_VND);
            CheckoutResponseData data = payOS.createPaymentLink(paymentData);
            checkoutUrl = data.getCheckoutUrl();
        } catch (Exception e) {
            log.error("❌ PayOS Error (GOLD): {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION,
                    "Không thể tạo link thanh toán PayOS. Chi tiết: " + e.getMessage());
        }

        SubscriptionOrder order = SubscriptionOrder.builder()
                .user(user)
                .orderCode(orderCode)
                .amount(GOLD_PRICE_VND)
                .voucherCode(sanitizeVoucherCode(voucherCode))
                .status(SubscriptionOrder.SubscriptionStatus.PENDING)
                .planTarget(UserPlan.GOLD)
                .build();
        subscriptionOrderRepository.save(order);

        return GoldCheckoutResponse.builder()
                .orderCode(orderCode)
                .checkoutUrl(checkoutUrl)
                .amount(GOLD_PRICE_VND)
                .build();
    }

    private boolean isGoldEffective(User user) {
        if (user.getRoles() != null) {
            boolean isAdminOrExpert = user.getRoles().stream().anyMatch(r -> {
                String name = r.getName();
                return "ADMIN".equalsIgnoreCase(name) || "EXPERT".equalsIgnoreCase(name)
                        || "ROLE_ADMIN".equalsIgnoreCase(name) || "ROLE_EXPERT".equalsIgnoreCase(name);
            });
            if (isAdminOrExpert) return true;
        }
        return user.getPlan() == UserPlan.GOLD;
    }

    private long generateUniqueOrderCode() {
        for (int attempt = 0; attempt < 30; attempt++) {
            long code = (System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(0, 10_000)) % 10000000000L;
            if (code < 1000000000L) code += 1000000000L; // ensure at least 10 digits

            boolean existsInWorkshops = workshopBookingRepository.findByOrderCode(code).isPresent();
            boolean existsInSubs = subscriptionOrderRepository.findByOrderCode(code).isPresent();
            if (!existsInWorkshops && !existsInSubs) return code;
        }

        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể tạo orderCode hợp lệ. Vui lòng thử lại.");
    }

    private String sanitizeVoucherCode(String voucherCode) {
        if (voucherCode == null) return null;
        String trimmed = voucherCode.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > 50) trimmed = trimmed.substring(0, 50);
        return trimmed;
    }

    @Transactional
    public void processWebhook(com.fasterxml.jackson.databind.JsonNode requestBody) {
        if (requestBody == null || !requestBody.has("data")) {
            log.warn("🔔 Skipping empty or invalid PayOS Webhook");
            return;
        }

        com.fasterxml.jackson.databind.JsonNode data = requestBody.get("data");
        long orderCode = data.get("orderCode").asLong();
        String status = data.has("status") ? data.get("status").asText() : "";

        log.info("🔔 Processing Webhook Data - OrderCode: {}, Status: {}", orderCode, status);

        if ("PAID".equalsIgnoreCase(status)) {
            SubscriptionOrder order = subscriptionOrderRepository.findByOrderCode(orderCode)
                    .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không tìm thấy Đơn hàng nâng cấp."));

            if (order.getStatus() == SubscriptionOrder.SubscriptionStatus.PENDING) {
                // 1. Update Order Status
                order.setStatus(SubscriptionOrder.SubscriptionStatus.PAID);
                order.setPaidAt(java.time.LocalDateTime.now());
                subscriptionOrderRepository.save(order);

                // 2. Upgrade User Tier
                User user = order.getUser();
                if (user != null) {
                    user.setPlan(UserPlan.GOLD);
                    userRepository.save(user);
                    log.info("✅ SUCCESS: User '{}' upgraded to GOLD.", user.getUsername());
                } else {
                    log.error("❌ Order {} has NO user associated!", orderCode);
                }
            } else {
                log.info("ℹ️ Order {} is already in status: {}. Skipping.", orderCode, order.getStatus());
            }
        }
    }
}
