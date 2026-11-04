package com.mathtutor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

/**
 * Entidad JPA para cuota de uso diaria.
 */
@Entity
@Table(name = "usage_quotas", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quota_day"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quota_day", nullable = false)
    private LocalDate day;

    @Column(name = "tokens_used", nullable = false)
    @Builder.Default
    private long tokensUsed = 0;

    @Column(name = "token_limit", nullable = false)
    @Builder.Default
    private long tokenLimit = 10000;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;
}