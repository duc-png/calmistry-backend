package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.service.CommentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentController {
    CommentService commentService;

    @PostMapping("/{id}/like")
    public ApiResponse<Void> likeComment(@PathVariable Long id) {
        commentService.likeComment(id);
        return ApiResponse.<Void>builder()
                .message("Liked comment successfully")
                .build();
    }

    @DeleteMapping("/{id}/like")
    public ApiResponse<Void> unlikeComment(@PathVariable Long id) {
        commentService.unlikeComment(id);
        return ApiResponse.<Void>builder()
                .message("Unliked comment successfully")
                .build();
    }
}
