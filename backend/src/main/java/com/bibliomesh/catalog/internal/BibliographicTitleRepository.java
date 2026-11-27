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
}