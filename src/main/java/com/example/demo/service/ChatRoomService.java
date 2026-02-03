package com.example.demo.service;

import com.example.demo.dto.request.ChatRoomRequest;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.User;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.ChatRoomRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public List<ChatMessage> getMessages(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getType() == ChatRoom.ChatRoomType.GROUP) {
            User currentUser = getCurrentUser();
            if (room.getMembers().stream().noneMatch(m -> m.getId().equals(currentUser.getId()))) {
                room.getMembers().add(currentUser);
                chatRoomRepository.save(room);
            }
        }
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
    }

    public ChatRoom createRoom(ChatRoomRequest request) {
        User creator = getCurrentUser();

        ChatRoom room = new ChatRoom();
        room.setName(request.getName());
        room.setDescription(request.getDescription());
        room.setType(request.getType() != null ? request.getType() : ChatRoom.ChatRoomType.GROUP);
        room.setCreator(creator);
        room.setStatus(ChatRoom.ChatRoomStatus.ACTIVE);

        // Add creator as the first member
        room.getMembers().add(creator);

        return chatRoomRepository.save(room);
    }

    public List<ChatRoom> getAllRooms() {
        return chatRoomRepository.findAll();
    }

    public ChatRoom addMember(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        room.getMembers().add(user);
        return chatRoomRepository.save(room);
    }

    public ChatRoom removeMember(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        room.getMembers().remove(user);
        return chatRoomRepository.save(room);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
