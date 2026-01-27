package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.ChatRequest;
import com.example.demo.dto.response.ChatHistoryResponse;
import com.example.demo.dto.response.ChatResponse;
import com.example.demo.service.AiChatService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-chat")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiChatController {

    AiChatService aiChatService;

    @PostMapping("/send")
    public ApiResponse<ChatResponse> sendMessage(@RequestBody @Valid ChatRequest request) {
        var result = aiChatService.sendMessage(request);
        return ApiResponse.<ChatResponse>builder()
                .result(result)
                .build();
    }

    @GetMapping("/history")
    public ApiResponse<ChatHistoryResponse> getChatHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = aiChatService.getChatHistory(page, size);
        return ApiResponse.<ChatHistoryResponse>builder()
                .result(result)
                .build();
    }

    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory() {
        aiChatService.clearHistory();
        return ApiResponse.<Void>builder()
                .message("Chat history cleared successfully")
                .build();
    }
}
