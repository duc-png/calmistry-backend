package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.ChatRoom;
import com.example.demo.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;

    @GetMapping
    public ApiResponse<List<ChatRoom>> getAllRooms() {
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        return ApiResponse.<List<ChatRoom>>builder()
                .code(1000)
                .result(rooms)
                .build();
    }
}
