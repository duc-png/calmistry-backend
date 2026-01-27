package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chat_rooms", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "expert_id" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ChatRoomType type = ChatRoomType.PRIVATE;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "expert_id")
    private ExpertProfile expert;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ChatRoomStatus status = ChatRoomStatus.ACTIVE;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private Set<ChatMessage> messages = new HashSet<>();

    public enum ChatRoomStatus {
        ACTIVE, CLOSED
    }

    public enum ChatRoomType {
        PRIVATE, GROUP
    }
}
