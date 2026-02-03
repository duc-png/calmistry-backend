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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SYSTEM_PROMPT = "Bạn là Calmistry AI, một trợ lý hỗ trợ sức khỏe tinh thần đầy lòng trắc ẩn. "
            +
            "Vai trò của bạn là: " +
            "- Lắng nghe một cách đồng cảm những lo lắng của người dùng " +
            "- Đưa ra những phản hồi hỗ trợ và khích lệ " +
            "- Đề xuất các chiến lược đối phó lành mạnh " +
            "- Không bao giờ chẩn đoán hoặc thay thế sự giúp đỡ chuyên nghiệp " +
            "- Giữ câu trả lời ngắn gọn và ấm áp " +
            "- Luôn trả lời bằng tiếng Việt";

    /**
     * Generate AI response using Gemini API (Native Endpoint)
     */
    public String generateResponse(String userMessage) {
        try {
            // Use native Gemini endpoint
            String url = String.format(
                    "https://generativelanguage.googleapis.com/v1/models/%s:generateContent?key=%s",
                    model, apiKey);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();

            // Add system prompt + user message
            String fullPrompt = SYSTEM_PROMPT + "\n\nNgười dùng: " + userMessage;

            Map<String, Object> part = new HashMap<>();
            part.put("text", fullPrompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));

            requestBody.put("contents", List.of(content));

            // Generation config
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 500);
            requestBody.put("generationConfig", generationConfig);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Call API
            log.info("Calling Gemini API with model: {} via native endpoint", model);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            // Extract response text (Native Gemini format)
            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (!candidates.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> content1 = (Map<String, Object>) candidates.get(0).get("content");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content1.get("parts");
                    if (!parts.isEmpty()) {
                        String aiResponse = (String) parts.get(0).get("text");
                        log.info("Successfully received AI response");
                        return aiResponse.trim();
                    }
                }
            }

            log.warn("Unexpected response format from Gemini API");
            return "Xin lỗi, tôi không thể trả lời lúc này. Vui lòng thử lại sau.";

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error("Rate limit exceeded: {}", e.getMessage());
            return "Xin lỗi, hệ thống đang quá tải. Vui lòng thử lại sau vài phút.";
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("API key invalid or expired: {}", e.getMessage());
            return "Xin lỗi, có vấn đề với cấu hình hệ thống. Vui lòng liên hệ quản trị viên.";
        } catch (HttpClientErrorException e) {
            log.error("HTTP error calling Gemini API: {} - {}", e.getStatusCode(), e.getMessage());
            return "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.";
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            return "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.";
        }
    }
}
