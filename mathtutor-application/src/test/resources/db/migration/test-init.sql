-- Script de inicialización para tests con pgvector
-- Crea la extensión vector si no existe

CREATE EXTENSION IF NOT EXISTS vector;

-- Comentario: pgvector extension para embeddings
COMMENT ON EXTENSION vector IS 'pgvector extension for vector similarity search';