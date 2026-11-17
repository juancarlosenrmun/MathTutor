package com.mathtutor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Turno completo y reserva de petición, también para conversaciones sin ejercicio. */
@Entity @Table(name = "conversation_turns", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "request_id"}))
@Getter @Setter @NoArgsConstructor
public class ConversationTurn {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "session_id", nullable = false) private TutorSession session;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "exercise_run_id") private ExerciseRun exerciseRun;
    @Column(name = "request_id", nullable = false) private UUID requestId;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Column(columnDefinition = "TEXT") private String response;
    private String topic;
    private String diagnosis;
    private String verdict;
    private String helpLevel;
    private long latencyMs;
    private Integer tokensIn;
    private Integer tokensOut;
    @Column(nullable = false) private String status;
    @Column(nullable = false) private Instant createdAt;
}
