package com.bibliomesh.schools.internal;

import com.bibliomesh.schools.School;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findByCodeIgnoreCaseAndActiveTrue(String code);

    @Modifying
    @Query(value = """
        UPDATE schools
        SET last_inscription_seq = last_inscription_seq + 1,
            updated_at = NOW()
        WHERE id = :schoolId
        RETURNING code, last_inscription_seq, seq_padding
        """, nativeQuery = true)
    List<Object[]> claimNextSequence(@Param("schoolId") Long schoolId);
}