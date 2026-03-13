package com.example.demo.service;

import com.example.demo.dto.request.WorkshopRequest;
import com.example.demo.dto.response.WorkshopResponse;
import com.example.demo.entity.User;
import com.example.demo.entity.Workshop;
import com.example.demo.entity.WorkshopBooking;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkshopBookingRepository;
import com.example.demo.repository.WorkshopRepository;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkshopService {
    WorkshopRepository workshopRepository;
    WorkshopBookingRepository workshopBookingRepository;
    UserRepository userRepository;
    PayOS payOS;

    @lombok.experimental.NonFinal
    @Value("${app.frontend-url:http://localhost:5173}")
    String frontendUrl;

    @Transactional
    public WorkshopResponse createWorkshop(WorkshopRequest request) {
        log.info("🔨 Creating new workshop: {}", request.getTitle());
        Workshop workshop = Workshop.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .speakerName(request.getSpeakerName())
                .speakerBio(request.getSpeakerBio())
                .maxParticipants(request.getMaxParticipants())
                .imageUrl(request.getImageUrl())
                .location(request.getLocation())
                .price(request.getPrice())
                .build();

        return mapToResponse(workshopRepository.save(workshop));
    }

    public List<WorkshopResponse> getAllWorkshops() {
        return workshopRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<WorkshopResponse> getUpcomingWorkshops() {
        String username = null;
        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                username = SecurityContextHolder.getContext().getAuthentication().getName();
            }
        } catch (Exception e) {
            // ignore
        }

        User user = null;
        if (username != null && !username.equals("anonymousUser")) {
            user = userRepository.findByUsername(username).orElse(null);
        }
        final User finalUser = user;

        return workshopRepository.findByStatus(Workshop.WorkshopStatus.UPCOMING).stream()
                .map(workshop -> {
                    WorkshopResponse response = mapToResponse(workshop);
                    if (finalUser != null) {
                        response.setIsBooked(workshopBookingRepository.existsByUserAndWorkshop(finalUser, workshop));
                    } else {
                        response.setIsBooked(false);
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkshopResponse updateWorkshop(Long id, WorkshopRequest request) {
        log.info("✏️ Updating workshop {}: {}", id, request.getTitle());
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSHOP_NOT_FOUND));

        workshop.setTitle(request.getTitle());
        workshop.setDescription(request.getDescription());
        workshop.setStartTime(request.getStartTime());
        workshop.setEndTime(request.getEndTime());
        workshop.setSpeakerName(request.getSpeakerName());
        workshop.setSpeakerBio(request.getSpeakerBio());

        // Don't lower max participants below current participants
        if (request.getMaxParticipants() < workshop.getCurrentParticipants()) {
            throw new IllegalArgumentException("Cannot set max participants lower than current participants");
        }
        workshop.setMaxParticipants(request.getMaxParticipants());

        workshop.setImageUrl(request.getImageUrl());
        workshop.setLocation(request.getLocation());
        // For updates, price should probably only be editable if there are no bookings,
        // but for now we let admin change it.
        if (request.getPrice() != null) {
            workshop.setPrice(request.getPrice());
        }

        return mapToResponse(workshopRepository.save(workshop));
    }

    @Transactional
    public void deleteWorkshop(Long id) {
        log.info("🗑️ Deleting workshop {}", id);
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSHOP_NOT_FOUND));

        // Delete all bookings associated with this workshop first to avoid FK
        // constraint violations
        workshopBookingRepository.deleteByWorkshop(workshop);

        // Delete the workshop
        workshopRepository.delete(workshop);
        log.info("✅ Deleted workshop {}", id);
    }

    @Transactional
    public WorkshopResponse bookWorkshop(Long workshopId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("🔍 [Booking] User {} attempting to book workshop {}", username, workshopId);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSHOP_NOT_FOUND));

        // Basic validation with null safety
        int currentParticipants = workshop.getCurrentParticipants() != null ? workshop.getCurrentParticipants() : 0;
        int maxParticipants = workshop.getMaxParticipants() != null ? workshop.getMaxParticipants() : 999;
        log.info("📊 [Booking] Workshop: {}, Participants: {} / {}", workshopId, currentParticipants, maxParticipants);

        if (workshopBookingRepository.existsByUserAndWorkshop(user, workshop)) {
            log.warn("⚠️ [Booking] User {} already booked workshop {}", username, workshopId);
            throw new AppException(ErrorCode.WORKSHOP_ALREADY_BOOKED);
        }

        if (currentParticipants >= maxParticipants) {
            log.warn("⚠️ [Booking] Workshop {} is full ({} / {})", workshopId, currentParticipants, maxParticipants);
            throw new AppException(ErrorCode.WORKSHOP_FULL);
        }

        boolean isPaid = workshop.getPrice() != null && workshop.getPrice() > 0;
        log.info("💳 [Booking] Workshop {} isPaid: {}, price: {}", workshopId, isPaid, workshop.getPrice());

        WorkshopBooking.BookingStatus initialStatus = isPaid ? WorkshopBooking.BookingStatus.PENDING : WorkshopBooking.BookingStatus.CONFIRMED;

        WorkshopBooking booking = WorkshopBooking.builder()
                .user(user)
                .workshop(workshop)
                .bookedAt(LocalDateTime.now())
                .status(initialStatus)
                .build();
        log.info("📝 [Booking] Booking entity prepared for user {}", username);

        String checkoutUrl = null;

        if (isPaid) {
            // Generate order code (must be numeric and unique per PayOS rules, max 50 chars)
            long orderCode = System.currentTimeMillis() % 10000000000L; // 10 digits
            booking.setOrderCode(orderCode);
            log.info("🔢 [Booking] Generated OrderCode: {}", orderCode);
            
            // Calculate expired at (15 mins from now in Unix timestamp) - Robust to server timezone
            long expiredAt = Instant.now().getEpochSecond() + (15 * 60);

            // Sanitize title for PayOS (ASCII-safe, max 25 chars)
            String itemName = workshop.getTitle()
                    .replaceAll("[^a-zA-Z0-9\\s]", "")
                    .trim();
            if (itemName.length() > 25) itemName = itemName.substring(0, 25);
            if (itemName.isEmpty()) itemName = "Workshop";

            ItemData item = ItemData.builder()
                    .name(itemName)
                    .price(workshop.getPrice().intValue())
                    .quantity(1)
                    .build();

            // Strip trailing slash from frontendUrl to avoid double-slash
            String baseUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(workshop.getPrice().intValue())
                    .description("Thanh toan Workshop")
                    .returnUrl(baseUrl + "/workshops")
                    .cancelUrl(baseUrl + "/workshops")
                    .item(item)
                    .expiredAt(expiredAt)
                    .build();

            try {
                log.info("📦 Creating PayOS payment link. OrderCode: {}, Amount: {}", orderCode, workshop.getPrice().intValue());
                CheckoutResponseData data = payOS.createPaymentLink(paymentData);
                checkoutUrl = data.getCheckoutUrl();
            } catch (Exception e) {
                log.error("❌ PayOS Error: {}", e.getMessage(), e);
                // Return a clear error to the user
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, 
                    "Không thể tạo link thanh toán PayOS. Chi tiết: " + e.getMessage());
            }
        } else {
            // Update current participants only if firmly confirmed (free)
            int current = workshop.getCurrentParticipants() != null ? workshop.getCurrentParticipants() : 0;
            workshop.setCurrentParticipants(current + 1);
            workshopRepository.save(workshop);
        }

        workshopBookingRepository.save(booking);

        log.info("✅ User {} booked workshop {}. Status: {}", username, workshop.getTitle(), booking.getStatus());
        WorkshopResponse response = mapToResponse(workshop);
        response.setCheckoutUrl(checkoutUrl);
        return response;
    }

    @Transactional
    public void cancelBooking(Long workshopId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSHOP_NOT_FOUND));

        WorkshopBooking booking = workshopBookingRepository.findByUserAndWorkshop(user, workshop)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSHOP_NOT_BOOKED));

        if (java.time.Duration.between(booking.getBookedAt(), LocalDateTime.now()).toMinutes() > 60) {
            throw new AppException(ErrorCode.WORKSHOP_CANCEL_EXPIRED);
        }

        workshopBookingRepository.delete(booking);

        if (booking.getStatus() == WorkshopBooking.BookingStatus.CONFIRMED) {
            workshop.setCurrentParticipants(workshop.getCurrentParticipants() - 1);
            workshopRepository.save(workshop);
        }

        log.info("✅ User {} cancelled booking for workshop {}", username, workshop.getTitle());
    }

    private WorkshopResponse mapToResponse(Workshop workshop) {
        return WorkshopResponse.builder()
                .id(workshop.getId())
                .title(workshop.getTitle())
                .description(workshop.getDescription())
                .startTime(workshop.getStartTime())
                .endTime(workshop.getEndTime())
                .speakerName(workshop.getSpeakerName())
                .speakerBio(workshop.getSpeakerBio())
                .maxParticipants(workshop.getMaxParticipants())
                .currentParticipants(workshop.getCurrentParticipants())
                .status(workshop.getStatus().name())
                .imageUrl(workshop.getImageUrl())
                .location(workshop.getLocation())
                .price(workshop.getPrice())
                .build();
    }
}
