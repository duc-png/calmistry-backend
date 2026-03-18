package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.service.SubscriptionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payos")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PayosWebhookController {
    SubscriptionService subscriptionService;

    @PostMapping("/subscriptions/webhook")
    public ApiResponse<String> paymentWebhook(@RequestBody com.fasterxml.jackson.databind.JsonNode requestBody) {
        log.info("🔔 Received PayOS Webhook (via /payos): {}", requestBody);
        subscriptionService.processWebhook(requestBody);
        return ApiResponse.<String>builder()
                .code(1000)
                .result("Webhook processed successfully")
                .build();
    }
}
