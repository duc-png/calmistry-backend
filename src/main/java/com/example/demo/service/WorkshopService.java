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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
                .build();

        return mapToResponse(workshopRepository.save(workshop));
    }

    public List<WorkshopResponse> getAllWorkshops() {
        return workshopRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<WorkshopResponse> getUpcomingWorkshops() {
        return workshopRepository.findByStatus(Workshop.WorkshopStatus.UPCOMING).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkshopResponse bookWorkshop(Long workshopId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSHOP_NOT_FOUND));

        if (workshopBookingRepository.existsByUserAndWorkshop(user, workshop)) {
            throw new AppException(ErrorCode.WORKSHOP_ALREADY_BOOKED);
        }

        if (workshop.getCurrentParticipants() >= workshop.getMaxParticipants()) {
            throw new AppException(ErrorCode.WORKSHOP_FULL);
        }

        WorkshopBooking booking = WorkshopBooking.builder()
                .user(user)
                .workshop(workshop)
                .bookedAt(LocalDateTime.now())
                .status(WorkshopBooking.BookingStatus.CONFIRMED)
                .build();

        workshopBookingRepository.save(booking);

        // Update current participants
        workshop.setCurrentParticipants(workshop.getCurrentParticipants() + 1);
        if (workshop.getCurrentParticipants().equals(workshop.getMaxParticipants())) {
            // Optional: Auto change status to full/ongoing if needed
        }
        workshopRepository.save(workshop);

        log.info("✅ User {} booked workshop {}", username, workshop.getTitle());
        return mapToResponse(workshop);
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
                .build();
    }
}
