-- =============================================================================
-- BiblioMesh V3 — Escuela Holanda Pilot Fields & Didactic/Audiovisual Collection
-- =============================================================================

-- 1. Clasificación de lectura MEP y prefijo de signatura en títulos (ej. CR, R, I)
ALTER TABLE bibliographic_titles
    ADD COLUMN IF NOT EXISTS collection_prefix VARCHAR(16),
    ADD COLUMN IF NOT EXISTS is_recreational BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_recommended BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Condición física, origen de adquisición y auditoría física en ejemplares
ALTER TABLE book_copies
    ADD COLUMN IF NOT EXISTS physical_condition VARCHAR(20) NOT NULL DEFAULT 'FAIR'
        CHECK (physical_condition IN ('EXCELLENT', 'GOOD', 'FAIR', 'POOR')),
    ADD COLUMN IF NOT EXISTS acquisition_source VARCHAR(150),
    ADD COLUMN IF NOT EXISTS in_library BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS last_inventory_check_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS observations TEXT;

CREATE INDEX IF NOT EXISTS idx_book_copies_school_id ON book_copies (school_id);
CREATE INDEX IF NOT EXISTS idx_book_copies_in_library ON book_copies (in_library);

-- 3. Material audiovisual y didáctico (Fichas, Láminas, Mapas, DVD, Esferas, etc.)
CREATE TABLE IF NOT EXISTS audiovisual_resources (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT REFERENCES schools(id),
    inscription_number VARCHAR(64) NOT NULL UNIQUE,
    item_type VARCHAR(80) NOT NULL,
    description TEXT,
    author VARCHAR(200),
    publisher VARCHAR(200),
    country VARCHAR(100),
    publication_year VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'LOANED', 'DAMAGED', 'LOST', 'WITHDRAWN')),
    observations TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audiovisual_school_id ON audiovisual_resources (school_id);