-- Habilitar extensión pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Crear índices para búsqueda vectorial (opcional, se crean en la entidad)
-- CREATE INDEX IF NOT EXISTS idx_note_chunk_embedding ON note_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);