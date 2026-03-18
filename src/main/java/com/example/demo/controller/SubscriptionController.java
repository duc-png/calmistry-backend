package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.SubscriptionCheckoutRequest;
import com.example.demo.dto.response.GoldCheckoutResponse;
import com.example.demo.service.SubscriptionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionController {
    SubscriptionService subscriptionService;

    @PostMapping("/gold/checkout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<GoldCheckoutResponse> createGoldCheckout(@org.springframework.web.bind.annotation.RequestBody(required = false) SubscriptionCheckoutRequest request) {
        return ApiResponse.<GoldCheckoutResponse>builder()
                .result(subscriptionService.createGoldCheckout(request == null ? null : request.getVoucherCode()))
                .build();
    }

    @PostMapping("/webhook/payos")
    public ApiResponse<String> paymentWebhook(@org.springframework.web.bind.annotation.RequestBody com.fasterxml.jackson.databind.JsonNode requestBody) {
        log.info("🔔 Received PayOS Webhook: {}", requestBody);
        subscriptionService.processWebhook(requestBody);
        return ApiResponse.<String>builder()
                .code(1000)
                .result("Webhook processed successfully")
                .build();
    }
}
