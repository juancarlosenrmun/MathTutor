package com.mathtutor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA para concepto clave.
 */
@Entity
@Table(name = "key_concepts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeyConcept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
}