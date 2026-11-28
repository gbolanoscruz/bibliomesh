package com.bibliomesh.auth;

import com.bibliomesh.auth.AuthDtos.AdminSessionResponse;
import com.bibliomesh.auth.AuthDtos.LoginRequest;
import com.bibliomesh.auth.internal.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AdminSessionResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}