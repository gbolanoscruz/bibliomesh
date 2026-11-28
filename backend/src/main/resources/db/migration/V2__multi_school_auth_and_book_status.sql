CREATE TABLE IF NOT EXISTS schools (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(24) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    seq_padding INT NOT NULL DEFAULT 6,
    last_inscription_seq BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(30) NOT NULL,
    school_id BIGINT REFERENCES schools(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE books
    ADD COLUMN IF NOT EXISTS school_id BIGINT REFERENCES schools(id),
    ADD COLUMN IF NOT EXISTS sequence_number BIGINT,
    ADD COLUMN IF NOT EXISTS inscription_number VARCHAR(60),
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    ADD COLUMN IF NOT EXISTS unavailable_reason VARCHAR(50),
    ADD COLUMN IF NOT EXISTS unavailable_notes TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_books_inscription_number ON books(inscription_number);
CREATE UNIQUE INDEX IF NOT EXISTS ux_books_school_seq ON books(school_id, sequence_number);