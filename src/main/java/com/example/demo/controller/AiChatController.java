package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String response = aiChatService.chat(message);

        return ApiResponse.<String>builder()
                .code(1000)
                .result(response)
                .build();
    }

    @GetMapping("/history")
    public ApiResponse<org.springframework.data.domain.Page<com.example.demo.entity.AiChatLog>> getChatHistory(
            @org.springframework.data.web.PageableDefault(size = 50, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        return ApiResponse.<org.springframework.data.domain.Page<com.example.demo.entity.AiChatLog>>builder()
                .code(1000)
                .result(aiChatService.getChatHistory(pageable))
                .build();
    }
}
