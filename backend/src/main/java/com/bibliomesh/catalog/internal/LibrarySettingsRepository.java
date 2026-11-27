package com.bibliomesh.catalog.internal;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LibrarySettingsRepository extends JpaRepository<LibrarySettings, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM LibrarySettings s WHERE s.id = 1")
    Optional<LibrarySettings> findSingletonForUpdate();
}