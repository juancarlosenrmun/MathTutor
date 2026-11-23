ALTER TABLE exercise_runs ADD COLUMN last_activity_at TIMESTAMP WITH TIME ZONE;

UPDATE exercise_runs r
SET last_activity_at = GREATEST(r.started_at, r.ended_at,
    (SELECT MAX(t.created_at) FROM conversation_turns t WHERE t.exercise_run_id = r.id));

ALTER TABLE exercise_runs ALTER COLUMN last_activity_at SET NOT NULL;
ALTER TABLE exercise_runs ALTER COLUMN last_activity_at SET DEFAULT CURRENT_TIMESTAMP;
CREATE INDEX exercise_runs_active_session_idx ON exercise_runs(session_id, last_activity_at)
    WHERE state = 'COMENZADO';
