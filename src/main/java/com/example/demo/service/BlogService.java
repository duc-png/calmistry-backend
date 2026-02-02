package com.example.demo.service;

import com.example.demo.dto.request.BlogCreateRequest;
import com.example.demo.dto.request.BlogUpdateRequest;
import com.example.demo.dto.response.BlogResponse;
import com.example.demo.entity.Blog;
import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.entity.BlogCategory;
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
    com.example.demo.repository.BlogInteractionRepository blogInteractionRepository;
    com.example.demo.repository.CommentRepository commentRepository;

    @Transactional
    public BlogResponse createBlog(BlogCreateRequest request) {
        // Lấy username từ JWT token
        String username = SecurityUtil.getCurrentUsername();
        if (username == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Tìm user hiện tại
        User currentUser = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));

        // Kiểm tra permission (EXPERT or ADMIN)
        boolean canAutoPublish = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("EXPERT") || role.getName().equals("ADMIN"));

        // Kiểm tra category tồn tại
        BlogCategory category = blogCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        // Tạo blog mới
        Blog blog = new Blog();
        blog.setAuthor(currentUser); // Set user as author
        blog.setCategory(category);
        blog.setTitle(request.getTitle());
        blog.setContent(request.getContent());
        blog.setImageUrls(request.getImageUrls());

        // Set status based on role
        if (canAutoPublish) {
            // EXPERT/ADMIN: Auto Publish
            // If Expert, set Expert profile if available
            if (currentUser.getRoles().stream().anyMatch(r -> r.getName().equals("EXPERT"))) {
                expertProfileRepository.findByUserId(currentUser.getId())
                        .ifPresent(blog::setExpert);
            }

            blog.setStatus(request.getStatus() != null
                    ? Blog.BlogStatus.valueOf(request.getStatus().toString())
                    : Blog.BlogStatus.PUBLISHED);
        } else {
            // User: Always PENDING
            blog.setStatus(Blog.BlogStatus.PENDING);
        }

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

    @Transactional
    public BlogResponse updateBlog(Long id, BlogUpdateRequest request) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        User currentUser = getCurrentUser();

        // Permission check: ADMIN or Author
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));

        // Author check with fallback
        User author = blog.getAuthor();
        if (author == null && blog.getExpert() != null) {
            author = blog.getExpert().getUser();
        }

        boolean isOwner = author != null && author.getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            blog.setTitle(request.getTitle());
            // Update slug if title changed
            String slug = generateSlug(request.getTitle());
            String finalSlug = slug;
            int counter = 1;
            while (blogRepository.findBySlug(finalSlug).isPresent() && !finalSlug.equals(blog.getSlug())) {
                finalSlug = slug + "-" + counter++;
            }
            blog.setSlug(finalSlug);
        }

        if (request.getContent() != null && !request.getContent().isBlank()) {
            blog.setContent(request.getContent());
        }

        if (request.getCategoryId() != null) {
            BlogCategory category = blogCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
            blog.setCategory(category);
        }

        if (request.getImageUrls() != null) {
            blog.setImageUrls(request.getImageUrls());
        }

        if (request.getStatus() != null) {
            try {
                blog.setStatus(Blog.BlogStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        return mapToResponse(blogRepository.save(blog));
    }

    @Transactional
    public void deleteBlog(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        User currentUser = getCurrentUser();

        // Permission check: ADMIN or Author
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));

        // Author check with fallback
        User author = blog.getAuthor();
        if (author == null && blog.getExpert() != null) {
            author = blog.getExpert().getUser();
        }

        boolean isOwner = author != null && author.getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        blogRepository.delete(blog);
    }

    private boolean canManageBlog(Blog blog, User user) {
        if (user == null)
            return false;

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));

        // Ownership check with author fallback
        User author = blog.getAuthor();
        if (author == null && blog.getExpert() != null) {
            author = blog.getExpert().getUser();
        }

        boolean isOwner = author != null && author.getId().equals(user.getId());

        // Also check if assigned as expert (even if not author)
        boolean isAssignedExpert = blog.getExpert() != null && blog.getExpert().getUser() != null
                && blog.getExpert().getUser().getId().equals(user.getId());

        return isAdmin || isOwner || isAssignedExpert;
    }

    private User getCurrentUser() {
        String username = SecurityUtil.getCurrentUsername();
        if (username == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    public List<BlogResponse> getBlogsByExpert(Long expertId) {
        return blogRepository.findByExpertId(expertId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getAllCategories() {
        return blogCategoryRepository.findAll().stream()
                .map(category -> CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .description(category.getDescription())
                        .build())
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
        } else if (categoryId != null && status != null) {
            // Search theo category và status
            blogs = blogRepository.findByCategoryIdAndStatus(categoryId, status);
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

    @Transactional
    public BlogResponse approveBlog(Long id, Blog.BlogStatus status) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        // Validate status transition if needed
        blog.setStatus(status);

        // If approving, maybe set expert as the approver? Or just leave it.
        // For now just update status.

        return mapToResponse(blogRepository.save(blog));
    }

    @Transactional
    public void likeBlog(Long blogId) {
        String username = SecurityUtil.getCurrentUsername();
        if (username == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        User currentUser = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        if (!blogInteractionRepository.existsByUserAndBlogAndInteractionType(currentUser, blog,
                com.example.demo.entity.BlogInteraction.InteractionType.LIKE)) {
            com.example.demo.entity.BlogInteraction interaction = new com.example.demo.entity.BlogInteraction();
            interaction.setUser(currentUser);
            interaction.setBlog(blog);
            interaction.setInteractionType(com.example.demo.entity.BlogInteraction.InteractionType.LIKE);
            blogInteractionRepository.save(interaction);
        }
    }

    @Transactional
    public void unlikeBlog(Long blogId) {
        String username = SecurityUtil.getCurrentUsername();
        if (username == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        User currentUser = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        blogInteractionRepository
                .findByUserAndBlogAndInteractionType(currentUser, blog,
                        com.example.demo.entity.BlogInteraction.InteractionType.LIKE)
                .ifPresent(blogInteractionRepository::delete);
    }

    private BlogResponse mapToResponse(Blog blog) {
        String username = SecurityUtil.getCurrentUsername();
        User currentUser = null;
        if (username != null) {
            currentUser = userRepository.findByUsername(username)
                    .orElse(null);
        }

        long likeCount = blogInteractionRepository.countByBlogAndInteractionType(blog,
                com.example.demo.entity.BlogInteraction.InteractionType.LIKE);
        long commentCount = commentRepository.countByBlog(blog);
        boolean isLiked = currentUser != null && blogInteractionRepository.existsByUserAndBlogAndInteractionType(
                currentUser, blog, com.example.demo.entity.BlogInteraction.InteractionType.LIKE);

        // Calculate author fallback
        User author = blog.getAuthor();
        if (author == null && blog.getExpert() != null) {
            author = blog.getExpert().getUser();
        }

        return BlogResponse.builder()
                .id(blog.getId())
                .expertId(blog.getExpert() != null ? blog.getExpert().getId() : null)
                .expertName(blog.getExpert() != null && blog.getExpert().getUser() != null
                        ? blog.getExpert().getUser().getFullName()
                        : null)
                .categoryId(blog.getCategory() != null ? blog.getCategory().getId() : null)
                .categoryName(blog.getCategory() != null ? blog.getCategory().getName() : null)
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .content(blog.getContent())
                .imageUrls(blog.getImageUrls())
                .viewCount(blog.getViewCount())

                .likeCount(likeCount)
                .commentCount(commentCount)
                .isLiked(isLiked)

                .status(blog.getStatus())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                // Set Author details with fallback
                .authorName(author != null ? author.getFullName() : null)
                .authorId(author != null ? author.getId() : null)
                .authorAvatar("https://ui-avatars.com/api/?name="
                        + (author != null ? author.getFullName() : "User"))
                .expertUserId(blog.getExpert() != null && blog.getExpert().getUser() != null
                        ? blog.getExpert().getUser().getId()
                        : null)
                .build();
    }
}
