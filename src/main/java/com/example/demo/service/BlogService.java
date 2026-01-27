package com.example.demo.service;

import com.example.demo.dto.request.BlogCreateRequest;
import com.example.demo.dto.response.BlogResponse;
import com.example.demo.entity.Blog;
import com.example.demo.entity.BlogCategory;
import com.example.demo.entity.ExpertProfile;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.BlogCategoryRepository;
import com.example.demo.repository.BlogRepository;
import com.example.demo.repository.ExpertProfileRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.SecurityUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogService {
    BlogRepository blogRepository;
    BlogCategoryRepository blogCategoryRepository;
    ExpertProfileRepository expertProfileRepository;
    UserRepository userRepository;

    @Transactional
    public BlogResponse createBlog(BlogCreateRequest request) {
        // Lấy username từ JWT token
        String username = SecurityUtil.getCurrentUsername();
        if (username == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Tìm user hiện tại (có thể là username hoặc email)
        User currentUser = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));

        // Kiểm tra user có phải Expert không
        ExpertProfile expertProfile = expertProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        // Kiểm tra category tồn tại
        BlogCategory category = blogCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        // Tạo blog mới
        Blog blog = new Blog();
        blog.setExpert(expertProfile);
        blog.setCategory(category);
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setThumbnailUrl(request.getThumbnailUrl());
        blog.setStatus(request.getStatus() != null 
                ? Blog.BlogStatus.valueOf(request.getStatus().name()) 
                : Blog.BlogStatus.DRAFT);

        // Generate slug nếu không có
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(request.getTitle());
        }
        // Đảm bảo slug unique
        String finalSlug = slug;
        int counter = 1;
        while (blogRepository.findBySlug(finalSlug).isPresent()) {
            finalSlug = slug + "-" + counter++;
        }
        blog.setSlug(finalSlug);

        Blog savedBlog = blogRepository.save(blog);

        return mapToResponse(savedBlog);
    }

    public BlogResponse getBlogById(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        return mapToResponse(blog);
    }

    public List<BlogResponse> getAllBlogs() {
        return blogRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BlogResponse> getBlogsByExpert(Long expertId) {
        return blogRepository.findByExpertId(expertId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BlogResponse> getPublishedBlogs() {
        return blogRepository.findByStatus(Blog.BlogStatus.PUBLISHED).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BlogResponse> searchBlogs(String title, Long categoryId, Blog.BlogStatus status) {
        List<Blog> blogs;

        // Xử lý các trường hợp search khác nhau
        if (title != null && !title.isBlank() && categoryId != null && status != null) {
            // Search theo title, category và status
            blogs = blogRepository.findByTitleContainingIgnoreCaseAndCategoryIdAndStatus(
                    title, categoryId, status);
        } else if (title != null && !title.isBlank() && categoryId != null) {
            // Search theo title và category
            blogs = blogRepository.findByTitleContainingIgnoreCaseAndCategoryId(title, categoryId);
        } else if (title != null && !title.isBlank() && status != null) {
            // Search theo title và status
            blogs = blogRepository.findByTitleContainingIgnoreCaseAndStatus(title, status);
        } else if (title != null && !title.isBlank()) {
            // Chỉ search theo title
            blogs = blogRepository.findByTitleContainingIgnoreCase(title);
        } else if (categoryId != null) {
            // Chỉ search theo category
            blogs = blogRepository.findByCategoryId(categoryId);
        } else if (status != null) {
            // Chỉ search theo status
            blogs = blogRepository.findByStatus(status);
        } else {
            // Không có điều kiện nào, trả về tất cả
            blogs = blogRepository.findAll();
        }

        return blogs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateSlug(String title) {
        if (title == null || title.isBlank()) {
            return "blog-" + System.currentTimeMillis();
        }

        // Normalize và chuyển thành slug
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");

        slug = slug.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        return slug.isEmpty() ? "blog-" + System.currentTimeMillis() : slug;
    }

    private BlogResponse mapToResponse(Blog blog) {
        return BlogResponse.builder()
                .id(blog.getId())
                .expertId(blog.getExpert() != null ? blog.getExpert().getId() : null)
                .expertName(blog.getExpert() != null && blog.getExpert().getUser() != null
                        ? blog.getExpert().getUser().getFullName() : null)
                .categoryId(blog.getCategory() != null ? blog.getCategory().getId() : null)
                .categoryName(blog.getCategory() != null ? blog.getCategory().getName() : null)
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .content(blog.getContent())
                .thumbnailUrl(blog.getThumbnailUrl())
                .viewCount(blog.getViewCount())
                .status(blog.getStatus())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .build();
    }
}

