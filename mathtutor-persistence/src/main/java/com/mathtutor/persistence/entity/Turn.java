package com.mathtutor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entidad JPA para turno de conversación.
 */
@Entity
@Table(name = "turns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TurnType type;

    @Column(name = "user_input_latex", columnDefinition = "TEXT")
    private String userInputLatex;

    @Column(name = "tutor_response", columnDefinition = "TEXT")
    private String tutorResponse;

    @Column(name = "help_level", length = 20)
    private String helpLevel;

    @Column(name = "event_type", length = 30)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_run_id", nullable = false)
    private ExerciseRun exerciseRun;

    public enum TurnType {
        USUARIO,
        TUTOR,
        SISTEMA
    }
}