package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.BlogCreateRequest;
import com.example.demo.dto.response.BlogResponse;
import com.example.demo.entity.Blog;
import com.example.demo.service.BlogService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogController {
    BlogService blogService;

    @PostMapping
    @PreAuthorize("hasAuthority('Role_Chuyên gia') or hasAuthority('Role_EXPERT')")
    public ResponseEntity<ApiResponse<BlogResponse>> createBlog(@Valid @RequestBody BlogCreateRequest request) {
        BlogResponse response = blogService.createBlog(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BlogResponse>builder()
                        .result(response)
                        .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogResponse>> getBlogById(@PathVariable Long id) {
        BlogResponse response = blogService.getBlogById(id);
        return ResponseEntity.ok(ApiResponse.<BlogResponse>builder()
                .result(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BlogResponse>>> getAllBlogs() {
        List<BlogResponse> blogs = blogService.getAllBlogs();
        return ResponseEntity.ok(ApiResponse.<List<BlogResponse>>builder()
                .result(blogs)
                .build());
    }

    @GetMapping("/published")
    public ResponseEntity<ApiResponse<List<BlogResponse>>> getPublishedBlogs() {
        List<BlogResponse> blogs = blogService.getPublishedBlogs();
        return ResponseEntity.ok(ApiResponse.<List<BlogResponse>>builder()
                .result(blogs)
                .build());
    }

    @GetMapping("/expert/{expertId}")
    public ResponseEntity<ApiResponse<List<BlogResponse>>> getBlogsByExpert(@PathVariable Long expertId) {
        List<BlogResponse> blogs = blogService.getBlogsByExpert(expertId);
        return ResponseEntity.ok(ApiResponse.<List<BlogResponse>>builder()
                .result(blogs)
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<BlogResponse>>> searchBlogs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Blog.BlogStatus status) {
        List<BlogResponse> blogs = blogService.searchBlogs(title, categoryId, status);
        return ResponseEntity.ok(ApiResponse.<List<BlogResponse>>builder()
                .result(blogs)
                .build());
    }
}

