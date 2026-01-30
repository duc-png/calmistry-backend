package com.example.demo.service;

import com.example.demo.dto.request.CreateJournalRequest;
import com.example.demo.dto.request.UpdateJournalRequest;
import com.example.demo.dto.response.JournalResponse;
import com.example.demo.entity.Journal;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.JournalRepository;
import com.example.demo.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JournalService {

    JournalRepository journalRepository;
    UserRepository userRepository;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    /**
     * Get all journals for current user with optional filters
     * TEMP: Works without authentication for testing
     */
    public List<JournalResponse> getJournals(String search, String mood) {
        try {
            User user = getCurrentUser();
            List<Journal> journals = journalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            log.info("Found {} journals for user {}", journals.size(), user.getUsername());
            return journals.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // TEMP: No auth context, return all journals
            log.warn("No authentication, returning all journals for testing");
            List<Journal> journals = journalRepository.findAll();
            return journals.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Get single journal by ID (only if user owns it)
     */
    public JournalResponse getJournalById(Long id) {
        User user = getCurrentUser();

        Journal journal = journalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        return toResponse(journal);
    }

    /**
     * Create new journal entry
     */
    @Transactional
    public JournalResponse createJournal(CreateJournalRequest request) {
        User user = getCurrentUser();

        Journal journal = new Journal();
        journal.setUser(user);
        journal.setTitle(request.getTitle());
        journal.setContent(request.getContent());
        journal.setMood(request.getMood());

        journal = journalRepository.save(journal);

        return toResponse(journal);
    }

    /**
     * Update existing journal (only if user owns it)
     */
    @Transactional
    public JournalResponse updateJournal(Long id, UpdateJournalRequest request) {
        User user = getCurrentUser();

        Journal journal = journalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        // Update fields if provided
        if (request.getTitle() != null) {
            journal.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            journal.setContent(request.getContent());
        }
        if (request.getMood() != null) {
            journal.setMood(request.getMood());
        }

        journal = journalRepository.save(journal);

        return toResponse(journal);
    }

    /**
     * Delete journal (only if user owns it)
     */
    @Transactional
    public void deleteJournal(Long id) {
        User user = getCurrentUser();

        Journal journal = journalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        journalRepository.delete(journal);
    }

    /**
     * Convert Journal entity to JournalResponse DTO
     */
    private JournalResponse toResponse(Journal journal) {
        return JournalResponse.builder()
                .id(journal.getId())
                .title(journal.getTitle())
                .content(journal.getContent())
                .mood(journal.getMood())
                .createdAt(journal.getCreatedAt())
                .build();
    }
}
