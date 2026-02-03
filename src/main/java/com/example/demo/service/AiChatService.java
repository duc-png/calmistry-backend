package com.example.demo.service;

import com.example.demo.dto.request.ChatRequest;
import com.example.demo.dto.response.ChatHistoryResponse;
import com.example.demo.dto.response.ChatResponse;
import com.example.demo.entity.AiChatLog;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.AiChatLogRepository;
import com.example.demo.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiChatService {

        AiChatLogRepository aiChatLogRepository;
        UserRepository userRepository;
        GeminiService geminiService;

        /**
         * Send message to AI and save conversation
         */
        @Transactional
        public ChatResponse sendMessage(ChatRequest request) {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

                // Get AI response from Gemini
                String aiResponse = geminiService.generateResponse(request.getMessage());

                // Save to database
                AiChatLog chatLog = new AiChatLog();
                chatLog.setUser(user);
                chatLog.setUserMessage(request.getMessage());
                chatLog.setAiResponse(aiResponse);

                chatLog = aiChatLogRepository.save(chatLog);

                return toResponse(chatLog);
        }

        /**
         * Get chat history for current user
         */
        public ChatHistoryResponse getChatHistory(int page, int size) {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

                Pageable pageable = PageRequest.of(page, size);
                Page<AiChatLog> logsPage = aiChatLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

                List<ChatResponse> messages = logsPage.getContent().stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());

                return ChatHistoryResponse.builder()
                                .messages(messages)
                                .totalMessages((int) logsPage.getTotalElements())
                                .build();
        }

        /**
         * Clear all chat history for current user
         */
        @Transactional
        public void clearHistory() {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

                aiChatLogRepository.deleteByUserId(user.getId());
        }

        /**
         * Convert AiChatLog entity to ChatResponse DTO
         */
        private ChatResponse toResponse(AiChatLog chatLog) {
                return ChatResponse.builder()
                                .id(chatLog.getId())
                                .userMessage(chatLog.getUserMessage())
                                .aiResponse(chatLog.getAiResponse())
                                .createdAt(chatLog.getCreatedAt())
                                .build();
        }
}
