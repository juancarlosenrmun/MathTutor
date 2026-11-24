-- Totales por catálogo de los ejercicios pendientes al borrar una conversación.
CREATE TABLE exercise_deletion_counts (
    module TEXT NOT NULL,
    topic TEXT NOT NULL,
    abandoned BIGINT NOT NULL CHECK (abandoned > 0),
    PRIMARY KEY (module, topic)
);
