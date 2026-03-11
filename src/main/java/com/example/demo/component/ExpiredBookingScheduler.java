package com.example.demo.component;

import com.example.demo.entity.WorkshopBooking;
import com.example.demo.repository.WorkshopBookingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpiredBookingScheduler {

    WorkshopBookingRepository workshopBookingRepository;

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    public void cancelExpiredPendingBookings() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
        List<WorkshopBooking> expiredBookings = workshopBookingRepository
                .findByStatusAndBookedAtBefore(WorkshopBooking.BookingStatus.PENDING, cutoffTime);

        if (!expiredBookings.isEmpty()) {
            log.info("Found {} pending bookings older than 15 mins. Cancelling them...", expiredBookings.size());
            for (WorkshopBooking booking : expiredBookings) {
                booking.setStatus(WorkshopBooking.BookingStatus.CANCELLED);
                // Depending on requirements, we can just delete it rather than keep a CANCELLED record
                // workshopBookingRepository.delete(booking);
            }
            workshopBookingRepository.saveAll(expiredBookings);
            log.info("Cancelled {} expired pending bookings.", expiredBookings.size());
        }
    }
}
