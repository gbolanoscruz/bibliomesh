package com.bibliomesh.auth.internal;

import com.bibliomesh.auth.AdminUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByEmailIgnoreCaseAndActiveTrue(String email);
}