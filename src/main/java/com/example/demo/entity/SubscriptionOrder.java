package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "subscription_orders",
        uniqueConstraints = @UniqueConstraint(columnNames = { "order_code" })
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SubscriptionOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_code", nullable = false)
    private Long orderCode;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_target", nullable = false, length = 10)
    @Builder.Default
    private UserPlan planTarget = UserPlan.GOLD;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum SubscriptionStatus {
        PENDING,
        PAID,
        CANCELLED
    }
}
