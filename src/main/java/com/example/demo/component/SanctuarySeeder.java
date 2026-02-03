package com.example.demo.component;

import com.example.demo.entity.ChatRoom;
import com.example.demo.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SanctuarySeeder implements CommandLineRunner {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    public void run(String... args) {
        seedRoom("Trạm Dừng Chân", "Muộn phiền để lại sau cánh cửa");
        seedRoom("Góc Tâm Tình", "Chia sẻ sâu sắc, thấu cảm chân thành");
        seedRoom("Khu Vườn Biết Ơn", "Hạt mầm tích cực mỗi ngày");
    }

    private void seedRoom(String name, String description) {
        if (chatRoomRepository.findByName(name).isEmpty()) {
            ChatRoom room = new ChatRoom();
            room.setName(name);
            room.setType(ChatRoom.ChatRoomType.GROUP);
            room.setStatus(ChatRoom.ChatRoomStatus.ACTIVE);
            // We use the name to identify, but the client will need IDs
            chatRoomRepository.save(room);
            log.info("🌸 Seeded Sanctuary Room: {}", name);
        }
    }
}
