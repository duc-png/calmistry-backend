package com.example.demo.service;

import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.ChatRoom;
import com.example.demo.entity.User;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.ChatRoomRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessage saveMessage(ChatMessage message) {
        // Fetch persistent entities using IDs from payload
        if (message.getRoom() == null || message.getRoom().getId() == null) {
            throw new IllegalArgumentException("Chat Room ID is required");
        }
        if (message.getSender() == null || message.getSender().getId() == null) {
            throw new IllegalArgumentException("Sender ID is required");
        }

        ChatRoom room = chatRoomRepository.findById(message.getRoom().getId())
                .orElseThrow(() -> new RuntimeException("ChatRoom not found: " + message.getRoom().getId()));

        User sender = userRepository.findById(message.getSender().getId())
                .orElseThrow(() -> new RuntimeException("User not found: " + message.getSender().getId()));

        message.setRoom(room);
        message.setSender(sender);

        return chatMessageRepository.save(message);
    }
}
