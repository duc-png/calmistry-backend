package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.ChatRoomRequest;
import com.example.demo.entity.ChatMessage;
import com.example.demo.entity.ChatRoom;
import com.example.demo.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping
    public ApiResponse<List<ChatRoom>> getAllRooms() {
        return ApiResponse.<List<ChatRoom>>builder()
                .code(1000)
                .result(chatRoomService.getAllRooms())
                .build();
    }

    @GetMapping("/{roomId}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(@PathVariable Long roomId) {
        return ApiResponse.<List<ChatMessage>>builder()
                .code(1000)
                .result(chatRoomService.getMessages(roomId))
                .build();
    }

    @PostMapping
    public ApiResponse<ChatRoom> createRoom(@RequestBody ChatRoomRequest request) {
        return ApiResponse.<ChatRoom>builder()
                .code(1000)
                .result(chatRoomService.createRoom(request))
                .build();
    }

    @PostMapping("/{roomId}/members/{userId}")
    public ApiResponse<ChatRoom> addMember(@PathVariable Long roomId, @PathVariable Long userId) {
        return ApiResponse.<ChatRoom>builder()
                .code(1000)
                .result(chatRoomService.addMember(roomId, userId))
                .build();
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    public ApiResponse<ChatRoom> removeMember(@PathVariable Long roomId, @PathVariable Long userId) {
        return ApiResponse.<ChatRoom>builder()
                .code(1000)
                .result(chatRoomService.removeMember(roomId, userId))
                .build();
    }
}
