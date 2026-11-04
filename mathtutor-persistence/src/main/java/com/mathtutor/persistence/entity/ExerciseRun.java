package com.mathtutor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA para ejecución de ejercicio.
 */
@Entity
@Table(name = "exercise_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_latex", nullable = false, columnDefinition = "TEXT")
    private String statementLatex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunState state;

    @Column(nullable = false)
    private double progress;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_level", nullable = false, length = 20)
    private HelpLevel currentLevel;

    @Column(nullable = false)
    private int attempts;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TutorSession session;

    @OneToMany(mappedBy = "exerciseRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Turn> turns = new HashSet<>();

    public enum RunState {
        COMENZADO,
        COMPLETADO,
        ABANDONADO
    }

    public enum AbandonPhase {
        INICIO,
        CERCA_SOLUCION
    }

    public enum HelpLevel {
        CONCEPTUAL,
        PROCEDIMENTAL,
        EJEMPLO_ANALOGO,
        REVELACION
    }
}