package com.mathtutor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entidad JPA para registro de consentimiento.
 */
@Entity
@Table(name = "consent_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 30)
    private ConsentType type;

    @Column(nullable = false)
    private boolean accepted;

    @Column(name = "policy_version", nullable = false, length = 50)
    private String policyVersion;

    @CreationTimestamp
    @Column(name = "consented_at", nullable = false, updatable = false)
    private Instant consentedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum ConsentType {
        PRIVACIDAD,
        COOKIES_TECNICAS,
        COOKIES_NO_ESENCIALES
    }
}