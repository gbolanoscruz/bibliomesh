package com.bibliomesh.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) {}

    public record AdminSessionResponse(
        String token,
        String email,
        String fullName,
        String role,
        Long schoolId,
        String schoolCode
    ) {}
}