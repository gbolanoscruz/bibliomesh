-- =============================================================================
-- BiblioMesh V1 Initial Schema (8 core tables)
-- =============================================================================

-- 1. Library global settings & inscription sequence
CREATE TABLE library_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    library_code VARCHAR(32) NOT NULL UNIQUE,
    library_name VARCHAR(150) NOT NULL,
    inscription_prefix VARCHAR(16) NOT NULL DEFAULT 'LIB',
    inscription_sequence INTEGER NOT NULL DEFAULT 1 CHECK (inscription_sequence >= 1),
    logo_url TEXT,
    digital_library_drive_folder_id VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO library_settings (library_code, library_name, inscription_prefix, inscription_sequence)
VALUES ('LIB-LOCAL', 'BiblioMesh School Library', 'LIB', 1);

-- 2. Bibliographic titles (MARC21 core + JSONB extras, 1 title -> N copies)
CREATE TABLE bibliographic_titles (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(20),
    title VARCHAR(300) NOT NULL,
    subtitle VARCHAR(300),
    preferred_title VARCHAR(300),
    author VARCHAR(250),
    publisher VARCHAR(200),
    publication_place VARCHAR(150),
    publication_year VARCHAR(32),
    series VARCHAR(200),
    volume VARCHAR(64),
    dewey_code VARCHAR(64),
    cutter_code VARCHAR(64),
    descriptors TEXT[] NOT NULL DEFAULT '{}',
    synopsis TEXT,
    general_note TEXT,
    cover_url TEXT,
    marc_extras JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_titles_isbn ON bibliographic_titles (isbn) WHERE isbn IS NOT NULL;
CREATE INDEX idx_titles_dewey ON bibliographic_titles (dewey_code);
CREATE INDEX idx_titles_fts ON bibliographic_titles USING GIN (
    to_tsvector(
        'spanish',
        COALESCE(title, '') || ' ' ||
        COALESCE(subtitle, '') || ' ' ||
        COALESCE(author, '') || ' ' ||
        COALESCE(publisher, '')
    )
);

-- 3. Physical book copies (inscription barcode & spine label queue)
CREATE TABLE book_copies (
    id BIGSERIAL PRIMARY KEY,
    title_id BIGINT NOT NULL REFERENCES bibliographic_titles (id) ON DELETE RESTRICT,
    inscription_number VARCHAR(64) NOT NULL UNIQUE,
    barcode VARCHAR(64) UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'LOANED', 'LOANED_INTERLIBRARY', 'IN_REPAIR', 'LOST')),
    shelf_location VARCHAR(100),
    queued_for_label BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_book_copies_title_id ON book_copies (title_id);
CREATE INDEX idx_book_copies_status ON book_copies (status);

-- 4. Technological resources (laptops, projectors, tablets, etc.)
CREATE TABLE tech_resources (
    id BIGSERIAL PRIMARY KEY,
    asset_tag VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    resource_type VARCHAR(32) NOT NULL
        CHECK (resource_type IN (
            'LAPTOP', 'TABLET', 'PROYECTOR', 'IMPRESORA', 'PARLANTE',
            'CAMARA', 'TELEVISOR', 'PANTALLA', 'ROUTER', 'OTRO'
        )),
    brand VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(120),
    condition_note VARCHAR(120),
    observations TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'LOANED', 'MAINTENANCE', 'RETIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tech_resources_status ON tech_resources (status);

-- 5. Patrons (students, teachers, staff, and partner libraries for ILL)
CREATE TABLE patrons (
    id BIGSERIAL PRIMARY KEY,
    identification_number VARCHAR(64) NOT NULL UNIQUE,
    first_name VARCHAR(120) NOT NULL,
    first_surname VARCHAR(120),
    second_surname VARCHAR(120),
    patron_type VARCHAR(32) NOT NULL DEFAULT 'STUDENT'
        CHECK (patron_type IN ('STUDENT', 'TEACHER', 'STAFF', 'PARTNER_LIBRARY')),
    grade_section VARCHAR(64),
    phone VARCHAR(40),
    email VARCHAR(180),
    observations TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_patrons_active ON patrons (active);
CREATE INDEX idx_patrons_name ON patrons (first_name, first_surname);

-- 6. External loans & return history (books OR tech resources)
CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    loan_code VARCHAR(64) NOT NULL UNIQUE,
    patron_id BIGINT NOT NULL REFERENCES patrons (id) ON DELETE RESTRICT,
    copy_id BIGINT REFERENCES book_copies (id) ON DELETE RESTRICT,
    tech_resource_id BIGINT REFERENCES tech_resources (id) ON DELETE RESTRICT,
    loaned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    due_date DATE,
    returned_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'RETURNED', 'OVERDUE', 'LOST')),
    notes TEXT,
    CONSTRAINT chk_loan_single_item CHECK (
        (copy_id IS NOT NULL AND tech_resource_id IS NULL) OR
        (copy_id IS NULL AND tech_resource_id IS NOT NULL)
    )
);

CREATE INDEX idx_loans_status ON loans (status);
CREATE INDEX idx_loans_patron_id ON loans (patron_id);
CREATE INDEX idx_loans_copy_id ON loans (copy_id) WHERE copy_id IS NOT NULL;
CREATE INDEX idx_loans_tech_id ON loans (tech_resource_id) WHERE tech_resource_id IS NOT NULL;

-- 7. In-room reading consultations (quick barcode scan log)
CREATE TABLE room_consultations (
    id BIGSERIAL PRIMARY KEY,
    movement_code VARCHAR(64) NOT NULL UNIQUE,
    copy_id BIGINT NOT NULL REFERENCES book_copies (id) ON DELETE RESTRICT,
    patron_id BIGINT REFERENCES patrons (id) ON DELETE SET NULL,
    consulted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_room_consultations_date ON room_consultations (consulted_at DESC);

-- 8. Digital library resources (optional catalog alongside Drive folder)
CREATE TABLE digital_resources (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    author VARCHAR(250),
    category VARCHAR(120),
    resource_url TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);