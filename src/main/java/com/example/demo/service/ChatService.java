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
        // Ensure room exists or get default public room
        // For simplicity, we assume room ID 1 is the "Public" room for now
        // In a real app, you'd fetch the room based on message.getRoom().getId()

        // This is a simplified save. Ideally, you fetch persistent entities
        // using the IDs from the incoming message payload to ensure consistency.

        return chatMessageRepository.save(message);
    }
}
