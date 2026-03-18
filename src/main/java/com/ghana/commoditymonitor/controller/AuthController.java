package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.request.*;
import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.JwtResponseDto;
import com.ghana.commoditymonitor.service.AgentRegistrationService;
import com.ghana.commoditymonitor.service.AuthService;
import com.ghana.commoditymonitor.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final PasswordResetService passwordResetService;
    private final AgentRegistrationService agentRegistrationService;

    @PostMapping("/login")
    @Operation(summary = "Login a user", description = "Authenticates user and returns a JWT token")
    public ResponseEntity<ApiResponse<JwtResponseDto>> login(@Valid @RequestBody AuthRequestDto request) {
        log.info("REST request to login user: {}", request.username());
        JwtResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account.")
    public ResponseEntity<ApiResponse<JwtResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("REST request to register user: {}", request.username());
        JwtResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully", response));
    }

    // ── PASSWORD RESET ──────────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password reset")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        passwordResetService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("If an account exists for " + request.getEmail() + ", you will receive reset instructions shortly.", null));
    }

    @GetMapping("/reset-password/validate")
    @Operation(summary = "Validate a reset token")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@jakarta.websocket.server.PathParam("token") String token) {
        boolean isValid = passwordResetService.isTokenValid(token);
        return ResponseEntity.ok(ApiResponse.ok("Token validation complete", isValid));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Execute password reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password has been successfully updated. You can now log in.", null));
    }

    // ── FIELD AGENT REGISTRATION ──────────────────────────────────────────────

    @PostMapping("/register-agent")
    @Operation(summary = "Apply for a field agent account")
    public ResponseEntity<ApiResponse<Void>> registerAgent(@Valid @RequestBody RegisterAgentRequestDto request) {
        agentRegistrationService.registerAgent(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Application received. An administrator will review your request.", null));
    }
}
