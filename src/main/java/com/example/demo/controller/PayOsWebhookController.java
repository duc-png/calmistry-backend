package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.Workshop;
import com.example.demo.entity.WorkshopBooking;
import com.example.demo.repository.WorkshopBookingRepository;
import com.example.demo.repository.WorkshopRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.util.Optional;

@RestController
@RequestMapping("/payos")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PayOsWebhookController {

    PayOS payOS;
    WorkshopBookingRepository workshopBookingRepository;
    WorkshopRepository workshopRepository;

    @PostMapping("/webhook")
    public ApiResponse<String> handleWebhook(@RequestBody Object requestBody) {
        log.info("Received PayOS webhook");
        try {
            // Note: The structure here depends on the specific PayOS SDK version.
            // Let's assume the library's Webhook object can be parsed directly from the json body
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonBody = mapper.writeValueAsString(requestBody);
            
            // Re-parsing exactly as expected by PayOS might require mapping to their Webhook class first
            Webhook webhookDataRequest = mapper.readValue(jsonBody, Webhook.class);
            WebhookData data = payOS.verifyPaymentWebhookData(webhookDataRequest);

            if ("00".equals(data.getCode())) {
                log.info("Payment success for orderCode: {}", data.getOrderCode());
                Optional<WorkshopBooking> bookingOpt = workshopBookingRepository.findByOrderCode(data.getOrderCode());

                if (bookingOpt.isPresent()) {
                    WorkshopBooking booking = bookingOpt.get();
                    if (booking.getStatus() == WorkshopBooking.BookingStatus.PENDING) {
                        booking.setStatus(WorkshopBooking.BookingStatus.CONFIRMED);
                        workshopBookingRepository.save(booking);

                        Workshop workshop = booking.getWorkshop();
                        workshop.setCurrentParticipants(workshop.getCurrentParticipants() + 1);
                        workshopRepository.save(workshop);

                        log.info("Confirmed booking for workshop {} via PayOS order {}", workshop.getTitle(), data.getOrderCode());
                    }
                }
            }

            return ApiResponse.<String>builder()
                    .code(1000)
                    .result("Webhook processed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Failed to process PayOS webhook: " + e.getMessage(), e);
            return ApiResponse.<String>builder()
                    .code(9999)
                    .result("Webhook verification failed")
                    .build();
        }
    }
}
