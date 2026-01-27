package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.StoryCreateRequest;
import com.example.demo.dto.response.StoryResponse;
import com.example.demo.service.StoryService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryController {
    StoryService storyService;

    @PostMapping
    public ResponseEntity<ApiResponse<StoryResponse>> createStory(@Valid @RequestBody StoryCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.<StoryResponse>builder()
                .result(storyService.createStory(request))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoryResponse>>> getStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.<List<StoryResponse>>builder()
                .result(storyService.getStories(page, size))
                .build());
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> likeStory(@PathVariable Long id) {
        storyService.likeStory(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Success")
                .build());
    }
}
