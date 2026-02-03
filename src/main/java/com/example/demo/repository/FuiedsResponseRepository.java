package com.example.demo.repository;

import com.example.demo.entity.FuiedsResponse;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FuiedsResponseRepository extends JpaRepository<FuiedsResponse, Long> {

    // Find today's response for a user
    Optional<FuiedsResponse> findByUserAndResponseDate(User user, LocalDate responseDate);

    // Get user's responses ordered by date descending
    List<FuiedsResponse> findByUserOrderByResponseDateDesc(User user);

    // Get user's responses for last N days
    @Query("SELECT f FROM FuiedsResponse f WHERE f.user = :user AND f.responseDate >= :startDate ORDER BY f.responseDate DESC")
    List<FuiedsResponse> findByUserAndResponseDateAfter(User user, LocalDate startDate);

    // Get last 3 responses before a specific date for EMA calculation
    @Query(value = "SELECT * FROM fuieds_responses f WHERE f.user_id = :userId AND f.response_date < :beforeDate ORDER BY f.response_date DESC LIMIT 3", nativeQuery = true)
    List<FuiedsResponse> findTop3ByUserAndBeforeDate(Long userId, LocalDate beforeDate);

    // Count consecutive "good enough" days
    @Query("SELECT COUNT(f) FROM FuiedsResponse f WHERE f.user = :user AND f.isGoodEnough = true AND f.responseDate >= :startDate")
    Long countGoodEnoughDaysSince(User user, LocalDate startDate);
}
