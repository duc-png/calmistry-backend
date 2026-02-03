package com.example.demo.dto.request;

import com.example.demo.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomRequest {
    private String name;
    private String description;
    private ChatRoom.ChatRoomType type;
}
