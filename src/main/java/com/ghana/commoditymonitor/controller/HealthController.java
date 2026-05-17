package com.ghana.commoditymonitor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for application health checks.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "Endpoints for application monitoring and keep-alive")
public class HealthController {

    @Operation(summary = "Keep-alive ping endpoint", description = "Lightweight endpoint used by external monitors to prevent the application from sleeping.")
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.trace("Received keep-alive ping");
        return ResponseEntity.ok("pong");
    }
}
