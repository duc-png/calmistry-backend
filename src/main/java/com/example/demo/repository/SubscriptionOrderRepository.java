package com.example.demo.repository;

import com.example.demo.entity.SubscriptionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionOrderRepository extends JpaRepository<SubscriptionOrder, Long> {
    Optional<SubscriptionOrder> findByOrderCode(Long orderCode);
}

