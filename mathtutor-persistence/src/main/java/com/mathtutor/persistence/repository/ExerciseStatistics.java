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

    /** @brief Clasifica el estado de la práctica y su avance observable por separado. */
    public List<Summary> summarize(String module, Instant now) {
        return jdbc.query("""
                WITH attempts AS (
                    SELECT m.code AS module, COALESCE(t.code, '') AS topic,
                        CASE WHEN r.state = 'COMPLETADO' THEN 'COMPLETADO'
                             WHEN r.state <> 'COMENZADO' OR s.status <> 'ACTIVA'
                                  OR r.last_activity_at < ? THEN 'A_MEDIAS'
                             WHEN r.attempts > 0 OR r.verified_steps > 0 OR r.progress > 0 THEN 'EN_CURSO'
                             ELSE 'COMENZADO' END AS state,
                        CASE WHEN r.reference_solution IS NOT NULL THEN r.progress
                             WHEN r.attempts = 0 THEN 0.0 END AS progress
                    FROM exercise_runs r JOIN tutor_sessions s ON s.id = r.session_id
                    JOIN modules m ON m.id = s.module_id LEFT JOIN topics t ON t.id = r.topic_id
                ), retained AS (
                    SELECT module, topic, COUNT(*) AS total,
                        COUNT(*) FILTER (WHERE state = 'COMENZADO') AS started,
                        COUNT(*) FILTER (WHERE state = 'EN_CURSO') AS active,
                        COUNT(*) FILTER (WHERE state = 'COMPLETADO') AS completed,
                        COUNT(*) FILTER (WHERE state = 'A_MEDIAS') AS unfinished,
                        COUNT(*) FILTER (WHERE state <> 'COMPLETADO' AND progress < ?) AS early,
                        COUNT(*) FILTER (WHERE state <> 'COMPLETADO' AND progress >= ?) AS near_solution,
                        COUNT(*) FILTER (WHERE state <> 'COMPLETADO' AND progress IS NULL) AS unmeasured
                    FROM attempts GROUP BY module, topic
                )
                SELECT module, NULLIF(topic, '') AS topic, COALESCE(total, 0) + COALESCE(abandoned, 0) AS total,
                    started, active, completed, unfinished, abandoned, early, near_solution, unmeasured
                FROM retained FULL JOIN exercise_deletion_counts USING (module, topic)
                WHERE CAST(? AS text) IS NULL OR module = ?
                ORDER BY module, NULLIF(topic, '') NULLS LAST
                """, (row, index) -> new Summary(row.getString("module"), row.getString("topic"), row.getLong("total"),
                row.getLong("started"), row.getLong("active"), row.getLong("completed"), row.getLong("unfinished"),
                row.getLong("abandoned"), row.getLong("early"), row.getLong("near_solution"), row.getLong("unmeasured")),
                Timestamp.from(now.minus(inactivity)), nearSolution, nearSolution, module, module);
    }

    public record Summary(String module, String topic, long total, long started, long active, long completed,
                          long unfinished, long abandoned, long early, long nearSolution, long unmeasured) {}
}
