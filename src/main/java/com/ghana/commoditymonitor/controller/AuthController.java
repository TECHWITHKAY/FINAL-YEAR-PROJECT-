package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.request.AuthRequestDto;
import com.ghana.commoditymonitor.dto.request.RegisterRequestDto;
import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.JwtResponseDto;
import com.ghana.commoditymonitor.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for authentication and registration endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user login and registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login a user", description = "Authenticates user and returns a JWT token")
    public ResponseEntity<ApiResponse<JwtResponseDto>> login(@Valid @RequestBody AuthRequestDto request) {
        log.info("REST request to login user: {}", request.username());
        JwtResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new user", description = "Creates a new user account. Only accessible by ADMIN.")
    public ResponseEntity<ApiResponse<JwtResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("REST request to register user: {}", request.username());
        JwtResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully", response));
    }
}
