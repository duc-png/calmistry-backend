package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.BlogCreateRequest;
import com.example.demo.dto.request.BlogUpdateRequest;
import com.example.demo.dto.request.CommentRequest;
import com.example.demo.dto.response.BlogResponse;
import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.entity.Blog;
import com.example.demo.service.BlogService;
import com.example.demo.service.CommentService;
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
        CommentService commentService;

        @PostMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<BlogResponse>> createBlog(@Valid @RequestBody BlogCreateRequest request) {
                BlogResponse response = blogService.createBlog(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.<BlogResponse>builder()
                                                .result(response)
                                                .build());
        }

        @PutMapping("/{id}/approve")
        @PreAuthorize("hasRole('EXPERT') or hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<BlogResponse>> approveBlog(@PathVariable Long id,
                        @RequestParam Blog.BlogStatus status) {
                BlogResponse response = blogService.approveBlog(id, status);
                return ResponseEntity.ok(ApiResponse.<BlogResponse>builder()
                                .result(response)
                                .build());
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') or hasRole('EXPERT')")
        public ResponseEntity<ApiResponse<BlogResponse>> updateBlog(@PathVariable Long id,
                        @Valid @RequestBody BlogUpdateRequest request) {
                BlogResponse response = blogService.updateBlog(id, request);
                return ResponseEntity.ok(ApiResponse.<BlogResponse>builder()
                                .result(response)
                                .build());
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') or hasRole('EXPERT')")
        public ResponseEntity<ApiResponse<Void>> deleteBlog(@PathVariable Long id) {
                blogService.deleteBlog(id);
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                                .message("Blog deleted successfully")
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

        @GetMapping("/featured")
        public ResponseEntity<ApiResponse<List<BlogResponse>>> getFeaturedBlogs() {
                List<BlogResponse> blogs = blogService.getFeaturedBlogs();
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

        @GetMapping("/categories")
        public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
                return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                                .result(blogService.getAllCategories())
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

        // --- Interaction Endpoints ---

        @PostMapping("/{id}/like")
        public ResponseEntity<ApiResponse<Void>> likeBlog(@PathVariable Long id) {
                blogService.likeBlog(id);
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                                .message("Liked blog successfully")
                                .build());
        }

        @DeleteMapping("/{id}/like")
        public ResponseEntity<ApiResponse<Void>> unlikeBlog(@PathVariable Long id) {
                blogService.unlikeBlog(id);
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                                .message("Unliked blog successfully")
                                .build());
        }

        @GetMapping("/{id}/comments")
        public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(@PathVariable Long id) {
                return ResponseEntity.ok(ApiResponse.<List<CommentResponse>>builder()
                                .result(commentService.getCommentsByBlogId(id))
                                .build());
        }

        @PostMapping("/{id}/comments")
        public ResponseEntity<ApiResponse<CommentResponse>> createComment(@PathVariable Long id,
                        @RequestBody @Valid CommentRequest request) {
                return ResponseEntity.ok(ApiResponse.<CommentResponse>builder()
                                .result(commentService.createComment(id, request))
                                .build());
        }
}
