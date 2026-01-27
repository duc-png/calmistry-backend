package com.example.demo.config;

import com.example.demo.entity.ChatRoom;
import com.example.demo.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ChatRoomRepository chatRoomRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Fix Schema: Make user_id and expert_id nullable for Group Chat
        try {
            jdbcTemplate.execute("ALTER TABLE chat_rooms MODIFY COLUMN user_id BIGINT NULL");
            jdbcTemplate.execute("ALTER TABLE chat_rooms MODIFY COLUMN expert_id BIGINT NULL");
            System.out.println("Schema updated: user_id and expert_id are now nullable.");
        } catch (Exception e) {
            System.out.println("Schema update skipped or failed (might already be nullable): " + e.getMessage());
        }

        // Initialize Public Chat Room if not exists
        if (chatRoomRepository.count() == 0) {
            ChatRoom publicRoom = new ChatRoom();
            publicRoom.setName("Community");
            publicRoom.setType(ChatRoom.ChatRoomType.GROUP);
            publicRoom.setStatus(ChatRoom.ChatRoomStatus.ACTIVE);
            chatRoomRepository.save(publicRoom);
            System.out.println("Initialized Public Chat Room: Community");
        }
    }
}
