package com.mathtutor.persistence.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** @brief Calcula el seguimiento docente con proyecciones agregadas (RF-24). */
@Repository
public class ExerciseStatistics {
    private final JdbcTemplate jdbc;
    private final Duration inactivity;
    private final double nearSolution;

    public ExerciseStatistics(JdbcTemplate jdbc,
                              @Value("${tutor.statistics.inactivity-minutes:30}") long inactivityMinutes,
                              @Value("${tutor.statistics.near-solution-threshold:0.5}") double nearSolution) {
        if (inactivityMinutes <= 0 || !Double.isFinite(nearSolution) || nearSolution < 0 || nearSolution > 1) {
            throw new IllegalArgumentException("Umbrales de estadística no válidos");
        }
        this.jdbc = jdbc;
        this.inactivity = Duration.ofMinutes(inactivityMinutes);
        this.nearSolution = nearSolution;
    }

    /** @brief El límite exacto sigue siendo activo. */
    public boolean expired(Instant lastActivity, Instant now) {
        return lastActivity.isBefore(now.minus(inactivity));
    }

    /** @brief Aplica la inactividad a sesiones aunque carezcan de un turno posterior. */
    public List<Summary> summarize(String module, Instant now) {
        return jdbc.query("""
                WITH attempts AS (
                    SELECT m.code AS module, t.code AS topic,
                        CASE WHEN r.state = 'COMENZADO' AND r.last_activity_at < ? THEN 'ABANDONADO'
                             ELSE r.state END AS state,
                        CASE WHEN r.reference_solution IS NOT NULL THEN r.progress
                             WHEN r.current_level = 'REVELACION' THEN 1.0
                             WHEN r.current_level = 'EJEMPLO_ANALOGO' THEN 2.0 / 3
                             WHEN r.current_level = 'PROCEDIMENTAL' THEN 1.0 / 3
                             ELSE 0.0 END AS progress
                    FROM exercise_runs r JOIN tutor_sessions s ON s.id = r.session_id
                    JOIN modules m ON m.id = s.module_id LEFT JOIN topics t ON t.id = r.topic_id
                    WHERE CAST(? AS text) IS NULL OR m.code = ?
                )
                SELECT module, topic, COUNT(*) AS started,
                    COUNT(*) FILTER (WHERE state = 'COMENZADO') AS active,
                    COUNT(*) FILTER (WHERE state = 'COMPLETADO') AS completed,
                    COUNT(*) FILTER (WHERE state = 'ABANDONADO') AS abandoned,
                    COUNT(*) FILTER (WHERE state = 'ABANDONADO' AND progress >= ?) AS near_solution
                FROM attempts GROUP BY module, topic ORDER BY module, topic NULLS LAST
                """, (row, index) -> {
            long abandoned = row.getLong("abandoned");
            long near = row.getLong("near_solution");
            return new Summary(row.getString("module"), row.getString("topic"), row.getLong("started"),
                    row.getLong("active"), row.getLong("completed"), abandoned, abandoned - near, near);
        }, Timestamp.from(now.minus(inactivity)), module, module, nearSolution);
    }

    public record Summary(String module, String topic, long started, long active, long completed, long abandoned,
                          long abandonedEarly, long abandonedNearSolution) {}
}
