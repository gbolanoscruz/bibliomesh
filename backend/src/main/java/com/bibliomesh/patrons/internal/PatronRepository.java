package com.bibliomesh.patrons.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatronRepository extends JpaRepository<Patron, Long> {

    Optional<Patron> findByIdentificationNumberIgnoreCase(String identificationNumber);

    long countByActiveTrue();

    List<Patron> findByActiveTrueOrderByFirstNameAscFirstSurnameAsc();

    @Query("""
            SELECT p FROM Patron p
            WHERE p.active = true AND (
                LOWER(p.identificationNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.firstSurname, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.secondSurname, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.gradeSection, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            ORDER BY p.firstName ASC, p.firstSurname ASC
            """)
    List<Patron> searchActive(@Param("q") String query);
}