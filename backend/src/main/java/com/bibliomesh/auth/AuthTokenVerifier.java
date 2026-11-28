package com.bibliomesh.auth;

import com.bibliomesh.auth.internal.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthTokenVerifier {

    private final JwtService jwtService;

    public AuthTokenVerifier(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public AuthPrincipal requireAdmin(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta token Bearer");
        }
        String rawToken = authorizationHeader.substring("Bearer ".length()).trim();
        return jwtService.verifyToken(rawToken)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o expirado"));
    }
}