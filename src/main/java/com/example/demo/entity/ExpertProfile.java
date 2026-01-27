package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "expert_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpertProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "specialty", length = 100)
    private String specialty;

    @Column(name = "degree", length = 100)
    private String degree;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @OneToMany(mappedBy = "expert", cascade = CascadeType.ALL)
    private Set<Blog> blogs = new HashSet<>();

    @OneToMany(mappedBy = "expert", cascade = CascadeType.ALL)
    private Set<ChatRoom> chatRoomsAsExpert = new HashSet<>();
}

