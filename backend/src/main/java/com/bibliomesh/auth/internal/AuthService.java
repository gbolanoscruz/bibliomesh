package com.bibliomesh.auth.internal;

import com.bibliomesh.auth.AdminUser;
import com.bibliomesh.auth.AuthDtos.AdminSessionResponse;
import com.bibliomesh.auth.AuthDtos.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final AdminUserRepository repository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final JdbcTemplate jdbcTemplate;

    public AuthService(
        AdminUserRepository repository,
        PasswordService passwordService,
        JwtService jwtService,
        JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminSessionResponse login(LoginRequest request) {
        AdminUser user = repository.findByEmailIgnoreCaseAndActiveTrue(request.email().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (!passwordService.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        String schoolCode = null;
        if (user.getSchoolId() != null) {
            schoolCode = jdbcTemplate.query(
                "SELECT code FROM schools WHERE id = ?",
                rs -> rs.next() ? rs.getString("code") : null,
                user.getSchoolId()
            );
        }

        String token = jwtService.generateToken(user);
        return new AdminSessionResponse(
            token,
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            user.getSchoolId(),
            schoolCode
        );
    }
}