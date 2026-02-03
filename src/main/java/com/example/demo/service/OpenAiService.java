package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenAiService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SYSTEM_PROMPT = "Bạn là Calmistry AI, một trợ lý hỗ trợ sức khỏe tinh thần đầy lòng trắc ẩn. "
            +
            "Vai trò của bạn là: " +
            "- Lắng nghe một cách đồng cảm những lo lắng của người dùng. " +
            "- Đưa ra những phản hồi hỗ trợ và khích lệ. " +
            "- Đề xuất các chiến lược đối phó lành mạnh. " +
            "- Không bao giờ chẩn đoán hoặc thay thế sự giúp đỡ chuyên nghiệp. " +
            "- Giữ câu trả lời ngắn gọn và ấm áp. " +
            "- Luôn trả lời bằng tiếng Việt.";

    /**
     * Generate AI response using OpenAI Chat Completions API
     */
    public String generateResponse(String userMessage) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Messages
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            messages.add(Map.of("role", "user", "content", userMessage));

            // Request Body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 500);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Calling OpenAI API with model: {}", model);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String aiResponse = (String) message.get("content");
                    log.info("Successfully received OpenAI response");
                    return aiResponse.trim();
                }
            }

            log.warn("Unexpected response format from OpenAI API");
            return "Xin lỗi, tôi không thể trả lời lúc này. Vui lòng thử lại sau.";

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error("OpenAI Rate limit exceeded: {}", e.getMessage());
            return "Xin lỗi, hệ thống OpenAI đang quá tải. Vui lòng thử lại sau vài phút.";
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("OpenAI API key invalid: {}", e.getMessage());
            return "Xin lỗi, có vấn đề với xác thực tài khoản AI. Vui lòng liên hệ quản trị viên.";
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return "Xin lỗi, đã có lỗi khi kết nối với OpenAI GPT. Vui lòng thử lại sau.";
        }
    }
}
