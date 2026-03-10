package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.AbstractIntegrationTest;
import com.ghana.commoditymonitor.dto.request.AuthRequestDto;
import com.ghana.commoditymonitor.dto.request.RegisterRequestDto;
import com.ghana.commoditymonitor.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the AuthController.
 * Tests authentication and registration scenarios against a real PostgreSQL container.
 */
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void login_WithInvalidCredentials_ShouldReturn401() {
        AuthRequestDto request = new AuthRequestDto("nonexistent", "password123");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_WithSeededAdmin_ShouldReturn200() {
        AuthRequestDto request = new AuthRequestDto("admin", "admin123");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"success\":true");
        assertThat(response.getBody()).contains("\"token\"");
    }

    @Test
    void register_WithoutAuth_ShouldReturn401Or403() {
        RegisterRequestDto request = new RegisterRequestDto(
                "newuser", "password123", "new@test.com", Role.VIEWER);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, String.class);
        // Register requires ADMIN role, so unauthenticated should fail
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    void register_AsAdmin_ShouldReturn201() {
        // First, login as admin to get a token
        AuthRequestDto loginRequest = new AuthRequestDto("admin", "admin123");
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, String.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Extract token (simple string extraction)
        String body = loginResponse.getBody();
        assertThat(body).isNotNull();
        String token = body.split("\"token\":\"")[1].split("\"")[0];

        // Register a new user with the admin token
        RegisterRequestDto registerRequest = new RegisterRequestDto(
                "testuser", "securePass1", "testuser@test.com", Role.VIEWER);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequestDto> entity = new HttpEntity<>(registerRequest, headers);

        ResponseEntity<String> registerResponse = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST, entity, String.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).contains("\"success\":true");
    }
}
