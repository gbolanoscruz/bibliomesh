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

ALTER TABLE book_copies
    ADD COLUMN IF NOT EXISTS school_id BIGINT REFERENCES schools(id),
    ADD COLUMN IF NOT EXISTS sequence_number BIGINT,
    ADD COLUMN IF NOT EXISTS unavailable_reason VARCHAR(50),
    ADD COLUMN IF NOT EXISTS unavailable_notes TEXT;

-- Amplía el CHECK de status para permitir bajas lógicas (DAMAGED, ERROR_ENTRY, WITHDRAWN)
ALTER TABLE book_copies DROP CONSTRAINT IF EXISTS book_copies_status_check;
ALTER TABLE book_copies
    ADD CONSTRAINT book_copies_status_check
    CHECK (status IN (
        'AVAILABLE',
        'LOANED',
        'LOANED_INTERLIBRARY',
        'IN_REPAIR',
        'LOST',
        'DAMAGED',
        'ERROR_ENTRY',
        'WITHDRAWN'
    ));

CREATE UNIQUE INDEX IF NOT EXISTS ux_book_copies_school_seq
    ON book_copies(school_id, sequence_number)
    WHERE school_id IS NOT NULL AND sequence_number IS NOT NULL;