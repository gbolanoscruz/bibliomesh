package com.bibliomesh.catalog.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BibliographicTitleRepository extends JpaRepository<BibliographicTitle, Long> {

    Optional<BibliographicTitle> findFirstByIsbn(String isbn);

    @Query("""
            SELECT DISTINCT t FROM BibliographicTitle t
            LEFT JOIN BookCopy c ON c.title = t
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(t.author, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(t.isbn, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(c.inscriptionNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY t.title ASC
            """)
    List<BibliographicTitle> search(@Param("q") String query);

    @org.springframework.data.jpa.repository.Query(
        value = """
            SELECT bt.* FROM bibliographic_titles bt
            WHERE (
                :query IS NULL OR :query = '' OR
                LOWER(bt.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
                LOWER(COALESCE(bt.author, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
                LOWER(COALESCE(bt.isbn, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
                EXISTS (
                    SELECT 1 FROM book_copies bc_q
                    WHERE bc_q.title_id = bt.id
                      AND LOWER(bc_q.inscription_number) LIKE LOWER(CONCAT('%', :query, '%'))
                )
            )
            AND (
                :schoolCode IS NULL OR :schoolCode = '' OR
                EXISTS (
                    SELECT 1 FROM book_copies bc_s
                    JOIN schools s ON s.id = bc_s.school_id
                    WHERE bc_s.title_id = bt.id
                      AND UPPER(s.code) = UPPER(:schoolCode)
                )
            )
            ORDER BY bt.title ASC, bt.id ASC
            """,
        countQuery = """
            SELECT COUNT(*) FROM bibliographic_titles bt
            WHERE (
                :query IS NULL OR :query = '' OR
                LOWER(bt.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
                LOWER(COALESCE(bt.author, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
                LOWER(COALESCE(bt.isbn, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
                EXISTS (
                    SELECT 1 FROM book_copies bc_q
                    WHERE bc_q.title_id = bt.id
                      AND LOWER(bc_q.inscription_number) LIKE LOWER(CONCAT('%', :query, '%'))
                )
            )
            AND (
                :schoolCode IS NULL OR :schoolCode = '' OR
                EXISTS (
                    SELECT 1 FROM book_copies bc_s
                    JOIN schools s ON s.id = bc_s.school_id
                    WHERE bc_s.title_id = bt.id
                      AND UPPER(s.code) = UPPER(:schoolCode)
                )
            )
            """,
        nativeQuery = true
    )
    org.springframework.data.domain.Page<BibliographicTitle> searchPaged(
            @org.springframework.data.repository.query.Param("query") String query,
            @org.springframework.data.repository.query.Param("schoolCode") String schoolCode,
            org.springframework.data.domain.Pageable pageable);
}
