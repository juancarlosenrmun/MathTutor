package com.mathtutor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA para error típico.
 */
@Entity
@Table(name = "typical_errors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypicalError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "seed_hint", columnDefinition = "TEXT")
    private String seedHint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canonical_step_id", nullable = false)
    private CanonicalStep canonicalStep;
}