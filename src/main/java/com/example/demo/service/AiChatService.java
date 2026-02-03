package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiChatService {

        private final ChatClient chatClient;

        public AiChatService(ChatClient.Builder builder) {
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
                try {
                        return chatClient.prompt()
                                        .user(message)
                                        .call()
                                        .content();
                } catch (Exception e) {
                        log.error("❌ AI Chat Error: ", e);
                        return "Xin lỗi, hiện tại mình đang gặp chút gián đoạn trong kết nối. Bạn hãy thử lại sau ít phút nhé. ❤️";
                }
        }
}
