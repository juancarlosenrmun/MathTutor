package com.mathtutor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA para paso canónico.
 */
@Entity
@Table(name = "canonical_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanonicalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int position;

    @Column(name = "expression_latex", nullable = false, columnDefinition = "TEXT")
    private String expressionLatex;

    @Column(name = "equivalent_forms", columnDefinition = "TEXT")
    private String equivalentForms; // JSON array

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @OneToMany(mappedBy = "canonicalStep", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TypicalError> typicalErrors = new HashSet<>();
}