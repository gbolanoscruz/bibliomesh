package com.bibliomesh.auth.internal;

import com.bibliomesh.auth.AdminUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminUserRepository repository;
    private final PasswordService passwordService;
    private final String bootstrapEmail;
    private final String bootstrapPassword;
    private final String bootstrapName;

    public AdminBootstrapRunner(
        AdminUserRepository repository,
        PasswordService passwordService,
        @Value("${app.auth.bootstrap-email:}") String bootstrapEmail,
        @Value("${app.auth.bootstrap-password:}") String bootstrapPassword,
        @Value("${app.auth.bootstrap-name:BiblioMesh Super Admin}") String bootstrapName
    ) {
        this.repository = repository;
        this.passwordService = passwordService;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
        this.bootstrapName = bootstrapName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapEmail == null || bootstrapEmail.isBlank()
                || bootstrapPassword == null || bootstrapPassword.isBlank()) {
            return;
        }
        String email = bootstrapEmail.trim().toLowerCase();
        if (repository.findByEmailIgnoreCaseAndActiveTrue(email).isPresent()) {
            return;
        }

        AdminUser admin = new AdminUser();
        admin.setEmail(email);
        admin.setPasswordHash(passwordService.hash(bootstrapPassword));
        admin.setFullName(bootstrapName);
        admin.setRole("SUPER_ADMIN");
        admin.setSchoolId(null);
        admin.setActive(true);
        repository.save(admin);
        log.info("Bootstrapped initial SUPER_ADMIN account: {}", email);
    }
}