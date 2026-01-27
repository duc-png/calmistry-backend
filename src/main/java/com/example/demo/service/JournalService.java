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
     * Get all journals for current user with optional filters
     */
    public List<JournalResponse> getJournals(String search, String mood) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Journal> journals;

        if (search != null && !search.trim().isEmpty()) {
            // Search by title or content
            journals = journalRepository.findByUserIdAndTitleContainingOrUserIdAndContentContainingOrderByCreatedAtDesc(
                    user.getId(), search, user.getId(), search);
        } else if (mood != null && !mood.trim().isEmpty() && !mood.equals("all")) {
            // Filter by mood
            journals = journalRepository.findByUserIdAndMoodOrderByCreatedAtDesc(user.getId(), mood);
        } else {
            // Get all journals
            journals = journalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        }

        return journals.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get single journal by ID (only if user owns it)
     */
    public JournalResponse getJournalById(Long id) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Journal journal = journalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Journal not found or not
                                                                                         // owned by user

        return toResponse(journal);
    }

    /**
     * Create new journal entry
     */
    @Transactional
    public JournalResponse createJournal(CreateJournalRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

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
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Journal journal = journalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Journal not found or not
                                                                                         // owned by user

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
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Journal journal = journalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION)); // Journal not found or not
                                                                                         // owned by user

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
