package com.salon.controller;

import com.salon.dto.request.LoginRequest;
import com.salon.dto.request.RegisterRequest;
import com.salon.dto.response.AuthResponse;
import com.salon.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              jakarta.servlet.http.HttpServletRequest http) {
        String ip = http.getHeader("X-Forwarded-For") != null
            ? http.getHeader("X-Forwarded-For").split(",")[0].trim() : http.getRemoteAddr();
        return ResponseEntity.ok(authService.login(request, ip));
    }
}
