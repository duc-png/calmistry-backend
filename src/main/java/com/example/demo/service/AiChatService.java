package com.example.demo.service;

import com.example.demo.entity.AiChatLog;
import com.example.demo.entity.User;
import com.example.demo.repository.AiChatLogRepository;
import com.example.demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiChatService {

        private final ChatClient chatClient;
        private final AiChatLogRepository aiChatLogRepository;
        private final UserRepository userRepository;

        public AiChatService(ChatClient.Builder builder, AiChatLogRepository aiChatLogRepository,
                        UserRepository userRepository) {
                this.aiChatLogRepository = aiChatLogRepository;
                this.userRepository = userRepository;
                this.chatClient = builder
                                .defaultSystem("Bạn là Trợ lý Calmistry, một chuyên gia tâm lý ảo thân thiện, thấu cảm và điềm đạm. "
                                                +
                                                "Nhiệm vụ của bạn là lắng nghe, thấu hiểu và đưa ra những lời khuyên nhẹ nhàng về sức khỏe tinh thần, "
                                                +
                                                "giảm căng thẳng và cân bằng cuộc sống. Hãy trả lời bằng tiếng Việt, ngôn từ trị liệu và ấm áp.")
                                .build();
        }

        public String chat(String message) {
                log.info("📧 Sending message to AI: {}", message);
                String aiResponse;
                try {
                        aiResponse = chatClient.prompt()
                                        .user(message)
                                        .call()
                                        .content();
                } catch (Exception e) {
                        log.error("❌ AI Chat Error: ", e);
                        aiResponse = "Xin lỗi, hiện tại mình đang gặp chút gián đoạn trong kết nối. Bạn hãy thử lại sau ít phút nhé. ❤️";
                }

                // Persist the log if user is authenticated
                final String finalResponse = aiResponse;
                try {
                        String username = SecurityContextHolder.getContext().getAuthentication().getName();
                        if (username != null && !username.equals("anonymousUser")) {
                                userRepository.findByUsername(username).ifPresent(user -> {
                                        AiChatLog logEntry = new AiChatLog();
                                        logEntry.setUser(user);
                                        logEntry.setUserMessage(message);
                                        logEntry.setAiResponse(finalResponse);
                                        aiChatLogRepository.save(logEntry);
                                });
                        }
                } catch (Exception e) {
                        log.error("❌ Failed to save AI Chat log: ", e);
                }

                return aiResponse;
        }

        public Page<AiChatLog> getChatHistory(Pageable pageable) {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                return aiChatLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        }
}
