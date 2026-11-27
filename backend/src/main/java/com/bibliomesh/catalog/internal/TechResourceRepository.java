package com.bibliomesh.catalog.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechResourceRepository extends JpaRepository<TechResource, Long> {

    Optional<TechResource> findByAssetTagIgnoreCase(String assetTag);

    List<TechResource> findAllByOrderByNameAsc();

    @Query("""
            SELECT r FROM TechResource r
            WHERE LOWER(r.assetTag) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(r.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(r.brand, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(r.model, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(r.serialNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY r.name ASC
            """)
    List<TechResource> search(@Param("q") String query);
}