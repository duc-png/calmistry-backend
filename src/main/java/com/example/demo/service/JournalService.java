package com.example.demo.service;

import com.example.demo.dto.request.CreateJournalRequest;
import com.example.demo.dto.request.UpdateJournalRequest;
import com.example.demo.dto.response.JournalResponse;
import com.example.demo.dto.response.JournalStatsResponse;
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
    AiChatService aiChatService;

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
        User user = getCurrentUser();
        List<Journal> journals = journalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        log.info("Found {} journals for user {}", journals.size(), user.getUsername());
        return journals.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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

        // Generate AI Healing Response
        String aiResponseText = generateAiHealingResponse(request.getTitle(), request.getContent(), request.getMood());
        journal.setAiResponse(aiResponseText);

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

        // Generate/Update AI Healing Response
        String aiResponseText = generateAiHealingResponse(journal.getTitle(), journal.getContent(), journal.getMood());
        journal.setAiResponse(aiResponseText);

        journal = journalRepository.save(journal);

        return toResponse(journal);
    }

    @Transactional
    public void deleteJournal(Long id) {
        User user = getCurrentUser();

        Journal journal = journalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        journalRepository.delete(journal);
    }

    /**
     * Generate a creative daily writing prompt for the user
     */
    public String getAiPrompt() {
        String prompt = "Hãy đóng vai là một chuyên gia tâm lý Calmistry. " +
                "Hãy đưa ra 1 câu hỏi gợi mở hoặc 1 chủ đề viết nhật ký ngắn gọn (dưới 30 từ) " +
                "giúp người dùng khám phá bản thân hoặc cảm thấy bình yên hơn. " +
                "Chỉ trả lời câu hỏi/chủ đề đó bằng tiếng Việt, không kèm theo lời dẫn.";
        return aiChatService.chat(prompt);
    }

    /**
     * Get mood statistics and AI analysis for the current user
     */
    public JournalStatsResponse getJournalStats() {
        User user = getCurrentUser();
        List<Journal> journals = journalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        long happyCount = journals.stream().filter(j -> "happy".equals(j.getMood())).count();
        long neutralCount = journals.stream().filter(j -> "neutral".equals(j.getMood())).count();
        long sadCount = journals.stream().filter(j -> "sad".equals(j.getMood())).count();
        long total = journals.size();

        String aiAnalysis = "Bạn hiện chưa có đủ dữ liệu nhật ký để AI phân tích. Hãy viết thêm nhé! ❤️";

        if (total > 0) {
            String prompt = String.format(
                    "Dữ liệu nhật ký của người dùng: Tổng số bài: %d, Vui vẻ: %d, Bình thường: %d, Buồn: %d. " +
                            "Hãy phân tích xu hướng tâm trạng này và đưa ra lời khuyên, động viên ngắn gọn (dưới 100 từ). "
                            +
                            "Hãy đóng vai là chuyên gia Calmistry, ngôn từ thấu cảm, ấm áp và mang tính định hướng tích cực.",
                    total, happyCount, neutralCount, sadCount);
            aiAnalysis = aiChatService.chat(prompt);
        }

        return JournalStatsResponse.builder()
                .happyCount(happyCount)
                .neutralCount(neutralCount)
                .sadCount(sadCount)
                .totalEntries(total)
                .aiAnalysis(aiAnalysis)
                .build();
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
                .aiResponse(journal.getAiResponse())
                .createdAt(journal.getCreatedAt())
                .build();
    }

    /**
     * Helper to generate a compassionate AI response for a journal entry
     */
    private String generateAiHealingResponse(String title, String content, String mood) {
        String prompt = String.format(
                "Nội dung nhật ký: Title: %s, Content: %s, Mood: %s. " +
                        "Dựa trên nội dung này, hãy viết một lời phản hồi ngắn (2-3 câu), cực kỳ thấu cảm, ấm áp và mang tính chữa lành. "
                        +
                        "Hãy đóng vai là một người bạn tri kỷ hoặc chuyên gia tâm lý Calmistry luôn lắng nghe người dùng. "
                        +
                        "Ngôn từ phải nhẹ nhàng, chân thành và khích lệ.",
                title, content, mood);
        return aiChatService.chat(prompt);
    }
}
