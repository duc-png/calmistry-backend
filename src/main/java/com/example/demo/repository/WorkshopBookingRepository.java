package com.example.demo.repository;

import com.example.demo.entity.User;
import com.example.demo.entity.Workshop;
import com.example.demo.entity.WorkshopBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkshopBookingRepository extends JpaRepository<WorkshopBooking, Long> {
    List<WorkshopBooking> findByUser(User user);

    Optional<WorkshopBooking> findByUserAndWorkshop(User user, Workshop workshop);

    boolean existsByUserAndWorkshop(User user, Workshop workshop);
}
