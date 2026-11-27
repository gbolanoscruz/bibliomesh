package com.bibliomesh.catalog.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    List<BookCopy> findByTitleIdOrderByInscriptionNumberAsc(Long titleId);

    long countByTitleId(Long titleId);

    boolean existsByInscriptionNumberIgnoreCase(String inscriptionNumber);

    Optional<BookCopy> findByInscriptionNumberIgnoreCase(String inscriptionNumber);
}