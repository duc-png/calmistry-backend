package com.example.demo.service;

import com.example.demo.dto.request.StoryCreateRequest;
import com.example.demo.dto.response.StoryResponse;
import com.example.demo.entity.Story;
import com.example.demo.entity.StoryInteraction;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStats;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.StoryInteractionRepository;
import com.example.demo.repository.StoryRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserStatsRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryService {
    StoryRepository storyRepository;
    StoryInteractionRepository storyInteractionRepository;
    UserRepository userRepository;
    UserStatsRepository userStatsRepository;

    @Transactional
    public StoryResponse createStory(StoryCreateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Story story = Story.builder()
                .user(user)
                .content(request.getContent())
                .isAnonymous(request.getIsAnonymous() != null && request.getIsAnonymous())
                .likeCount(0)
                .build();

        story = storyRepository.save(story);

        // Award points (+10 FUED)
        updateUserPoints(user, 10);

        return mapToResponse(story, user);
    }

    public List<StoryResponse> getStories(int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElse(null);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Story> storyPage = storyRepository.findAll(pageRequest);

        return storyPage.getContent().stream()
                .map(story -> mapToResponse(story, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional
    public void likeStory(Long storyId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));

        boolean hasLiked = storyInteractionRepository.existsByStoryAndUserAndType(
                story, user, StoryInteraction.InteractionType.LIKE);

        if (hasLiked) {
            // Unlike
            StoryInteraction interaction = storyInteractionRepository.findByStoryAndUserAndType(
                    story, user, StoryInteraction.InteractionType.LIKE)
                    .orElseThrow();
            storyInteractionRepository.delete(interaction);
            story.setLikeCount(Math.max(0, story.getLikeCount() - 1));

            // Remove points from author (-2)
            updateUserPoints(story.getUser(), -2);
        } else {
            // Like
            StoryInteraction interaction = new StoryInteraction();
            interaction.setStory(story);
            interaction.setUser(user);
            interaction.setType(StoryInteraction.InteractionType.LIKE);
            storyInteractionRepository.save(interaction);
            story.setLikeCount(story.getLikeCount() + 1);

            // Award points to author (+2)
            updateUserPoints(story.getUser(), 2);
        }

        storyRepository.save(story);
    }

    private void updateUserPoints(User user, int points) {
        UserStats stats = userStatsRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    UserStats s = new UserStats();
                    s.setUser(user);
                    s.setTotalPoints(0);
                    return s;
                });

        stats.setTotalPoints(Math.max(0, stats.getTotalPoints() + points));
        stats.setLastActivityDate(java.time.LocalDate.now());
        userStatsRepository.save(stats);
    }

    private StoryResponse mapToResponse(Story story, User currentUser) {
        boolean isLiked = false;
        if (currentUser != null) {
            isLiked = storyInteractionRepository.existsByStoryAndUserAndType(
                    story, currentUser, StoryInteraction.InteractionType.LIKE);
        }

        String authorName = story.getIsAnonymous() ? "Ẩn danh" : story.getUser().getFullName();
        if (authorName == null)
            authorName = story.getUser().getUsername();

        String avatar = story.getIsAnonymous() ? "bi-incognito" : "bi-person-circle";

        return StoryResponse.builder()
                .id(story.getId())
                .author(authorName)
                .avatar(avatar)
                .content(story.getContent())
                .time(formatTimeAgo(story.getCreatedAt()))
                .hearts(story.getLikeCount())
                .isLiked(isLiked)
                .isAnonymous(story.getIsAnonymous())
                .build();
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();

        if (seconds < 60)
            return "Vừa xong";
        if (seconds < 3600)
            return (seconds / 60) + " phút trước";
        if (seconds < 86400)
            return (seconds / 3600) + " giờ trước";
        return (seconds / 86400) + " ngày trước";
    }
}
