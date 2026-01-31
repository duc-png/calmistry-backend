package com.example.demo.service;

import com.example.demo.dto.request.CommentRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.Blog;
import com.example.demo.entity.Comment;
import com.example.demo.entity.CommentLike;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.BlogRepository;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.SecurityUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentService {
    CommentRepository commentRepository;
    CommentLikeRepository commentLikeRepository;
    BlogRepository blogRepository;
    UserRepository userRepository;

    @Transactional
    public CommentResponse createComment(Long blogId, CommentRequest request) {
        String username = SecurityUtil.getCurrentUsername();
        if (username == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        User currentUser = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .user(currentUser)
                .blog(blog)
                .build();

        return mapToResponse(commentRepository.save(comment), currentUser);
    }

    public List<CommentResponse> getCommentsByBlogId(Long blogId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        String currentUsername = SecurityUtil.getCurrentUsername();
        User currentUser = currentUsername != null ? userRepository.findByUsername(currentUsername).orElse(null) : null;

        return commentRepository.findByBlogOrderByCreatedAtDesc(blog).stream()
                .map(comment -> mapToResponse(comment, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional
    public void likeComment(Long commentId) {
        String username = SecurityUtil.getCurrentUsername();
        if (username == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        User currentUser = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY)); // Using generic error for not found

        if (!commentLikeRepository.existsByUserAndComment(currentUser, comment)) {
            CommentLike like = CommentLike.builder()
                    .user(currentUser)
                    .comment(comment)
                    .build();
            commentLikeRepository.save(like);
        }
    }

    @Transactional
    public void unlikeComment(Long commentId) {
        String username = SecurityUtil.getCurrentUsername();
        if (username == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        User currentUser = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        commentLikeRepository.findByUserAndComment(currentUser, comment)
                .ifPresent(commentLikeRepository::delete);
    }

    private CommentResponse mapToResponse(Comment comment, User currentUser) {
        long likeCount = commentLikeRepository.countByComment(comment);
        boolean isLiked = currentUser != null && commentLikeRepository.existsByUserAndComment(currentUser, comment);

        User user = comment.getUser();
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                // Add other fields if necessary
                .build();

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .user(userResponse)
                .createdAt(comment.getCreatedAt())
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }
}
