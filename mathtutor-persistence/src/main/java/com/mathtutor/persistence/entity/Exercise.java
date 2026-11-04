package com.mathtutor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA para ejercicio del banco.
 */
@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExerciseType type;

    @Column(nullable = false)
    private int difficulty;

    @Column(name = "statement_latex", nullable = false, columnDefinition = "TEXT")
    private String statementLatex;

    @Column(name = "reference_solution_latex", nullable = false, columnDefinition = "TEXT")
    private String referenceSolutionLatex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private Module module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CanonicalStep> canonicalSteps = new HashSet<>();

    public enum ExerciseType {
        TEORICO,
        PROCEDIMENTAL,
        INTERPRETACION
    }
}