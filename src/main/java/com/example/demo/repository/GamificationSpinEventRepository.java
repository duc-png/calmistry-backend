package com.example.demo.repository;

import com.example.demo.entity.GamificationEventType;
import com.example.demo.entity.GamificationSpinEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface GamificationSpinEventRepository extends JpaRepository<GamificationSpinEvent, Long> {
    boolean existsByUser_IdAndEventTypeAndEventDate(Long userId, GamificationEventType eventType, LocalDate eventDate);
    List<GamificationSpinEvent> findAllByUser_IdAndEventDate(Long userId, LocalDate eventDate);
}
