package com.mathtutor.persistence.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/** @brief Proyecciones administrativas de uso, sin mensajes ni resúmenes de sesión. */
@Repository
public class AdminUsageRepository {
    private final JdbcTemplate jdbc;

    public AdminUsageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @brief Cuenta turnos completados con tema conocido y sesiones distintas por módulo y tema. */
    public List<TopicUsage> topics(long offset, int limit) {
        return jdbc.query("""
                SELECT m.code AS module, t.topic, COUNT(DISTINCT t.session_id) AS sessions, COUNT(*) AS turns
                FROM conversation_turns t JOIN tutor_sessions s ON s.id = t.session_id
                JOIN modules m ON m.id = s.module_id
                WHERE t.status = 'COMPLETADO' AND trim(t.topic) <> ''
                GROUP BY m.code, t.topic
                ORDER BY m.code, t.topic
                LIMIT ? OFFSET ?
                """, (row, index) -> new TopicUsage(row.getString("module"), row.getString("topic"),
                row.getLong("sessions"), row.getLong("turns")), limit, offset);
    }

    /** @brief Consulta metadatos de sesiones del perfil en orden estable, incluidas las antiguas sin módulo. */
    public List<SessionUsage> sessions(long userId, long offset, int limit) {
        return jdbc.query("""
                SELECT s.id, m.code AS module, s.status, s.started_at, s.last_activity_at,
                       (SELECT COUNT(*) FROM exercise_runs r WHERE r.session_id = s.id AND r.state = 'COMPLETADO')
                           AS completed_exercises
                FROM tutor_sessions s LEFT JOIN modules m ON m.id = s.module_id
                WHERE s.user_id = ?
                ORDER BY s.started_at DESC, s.id DESC
                LIMIT ? OFFSET ?
                """, (row, index) -> new SessionUsage(row.getLong("id"), row.getString("module"),
                row.getString("status"), row.getTimestamp("started_at").toInstant(),
                row.getTimestamp("last_activity_at").toInstant(), row.getLong("completed_exercises")), userId, limit, offset);
    }

    public record TopicUsage(String module, String topic, long sessions, long turns) {}

    public record SessionUsage(long id, String module, String status, Instant startedAt, Instant lastActivityAt, long completedExercises) {}
}
