package com.mathtutor.persistence.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** @brief Exportación y borrado de los registros propios, sin credenciales ni datos de terceros. */
@Repository
public class PersonalDataStore {
    private final JdbcTemplate jdbc;

    public PersonalDataStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @brief Una sentencia da una instantánea coherente de todos los conjuntos exportados. */
    public String export(long userId) {
        return jdbc.query("""
                WITH owned AS (SELECT * FROM tutor_sessions WHERE user_id = ?),
                runs AS (SELECT r.* FROM exercise_runs r JOIN owned s ON s.id = r.session_id)
                SELECT jsonb_build_object(
                    'profile', jsonb_build_object('email', u.email, 'role', u.role, 'createdAt', u.created_at),
                    'consents', COALESCE((SELECT jsonb_agg(to_jsonb(c) - 'user_id' ORDER BY c.id)
                        FROM consent_records c WHERE c.user_id = u.id), '[]'),
                    'sessions', COALESCE((SELECT jsonb_agg(to_jsonb(s) - 'user_id' ORDER BY s.id) FROM owned s), '[]'),
                    'exercises', COALESCE((SELECT jsonb_agg(to_jsonb(r) - 'reference_solution' ORDER BY r.id)
                        FROM runs r), '[]'),
                    'conversations', COALESCE((SELECT jsonb_agg(to_jsonb(t) - 'request_id' ORDER BY t.id)
                        FROM conversation_turns t JOIN owned s ON s.id = t.session_id), '[]'),
                    'interactions', COALESCE((SELECT jsonb_agg(to_jsonb(t) ORDER BY t.id)
                        FROM turns t JOIN runs r ON r.id = t.exercise_run_id), '[]'),
                    'usage', COALESCE((SELECT jsonb_agg(to_jsonb(q) - 'user_id' ORDER BY q.id)
                        FROM usage_quotas q WHERE q.user_id = u.id), '[]'),
                    'accountEvents', COALESCE((SELECT jsonb_agg(jsonb_build_object('action', a.action, 'at', a.created_at)
                        ORDER BY a.id) FROM audit_logs a
                        WHERE a.actor_id = u.id OR (a.target_type IN ('USER', 'User') AND a.target_id = u.id)), '[]'))
                FROM users u WHERE u.id = ? AND u.active
                """, rows -> rows.next() ? rows.getString(1) : null, userId, userId);
    }

    /** @brief Borra las dependencias de la cuenta. El llamador debe bloquear antes al usuario. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void eraseRelated(long userId) {
        jdbc.queryForList("SELECT id FROM tutor_sessions WHERE user_id = ? ORDER BY id FOR UPDATE", Long.class, userId);
        jdbc.update("DELETE FROM conversation_turns WHERE session_id IN (SELECT id FROM tutor_sessions WHERE user_id = ?)", userId);
        jdbc.update("""
                DELETE FROM turns t USING exercise_runs r, tutor_sessions s
                WHERE t.exercise_run_id = r.id AND r.session_id = s.id AND s.user_id = ?
                """, userId);
        jdbc.update("DELETE FROM exercise_runs WHERE session_id IN (SELECT id FROM tutor_sessions WHERE user_id = ?)", userId);
        jdbc.update("DELETE FROM tutor_sessions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM usage_quotas WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM consent_records WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM password_reset_tokens WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId);
        jdbc.update("UPDATE audit_logs SET actor_id = NULL WHERE actor_id = ?", userId);
        jdbc.update("UPDATE audit_logs SET target_id = NULL WHERE target_type IN ('USER', 'User') AND target_id = ?", userId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void eraseSession(long sessionId) {
        jdbc.update("""
                INSERT INTO exercise_deletion_counts (module, topic, abandoned)
                SELECT m.code, COALESCE(t.code, ''), COUNT(*)
                FROM exercise_runs r JOIN tutor_sessions s ON s.id = r.session_id
                JOIN modules m ON m.id = s.module_id LEFT JOIN topics t ON t.id = r.topic_id
                WHERE r.session_id = ? AND r.state <> 'COMPLETADO'
                GROUP BY m.code, COALESCE(t.code, '')
                ON CONFLICT (module, topic) DO UPDATE
                SET abandoned = exercise_deletion_counts.abandoned + EXCLUDED.abandoned
                """, sessionId);
        jdbc.update("DELETE FROM conversation_turns WHERE session_id = ?", sessionId);
        jdbc.update("DELETE FROM turns WHERE exercise_run_id IN (SELECT id FROM exercise_runs WHERE session_id = ?)", sessionId);
        jdbc.update("DELETE FROM exercise_runs WHERE session_id = ?", sessionId);
        jdbc.update("DELETE FROM tutor_sessions WHERE id = ?", sessionId);
    }

    /** @brief Serializa cambios que pueden afectar al conjunto de administradores activos. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockAccountChanges() {
        jdbc.query("SELECT pg_advisory_xact_lock(80725)", rows -> { });
    }
}
