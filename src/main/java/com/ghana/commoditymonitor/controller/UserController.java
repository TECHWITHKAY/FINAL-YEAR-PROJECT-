package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.UserResponseDto;
import com.ghana.commoditymonitor.entity.User;
import com.ghana.commoditymonitor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for managing user accounts.
 * Restricted to administrators only.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing user accounts and activation (Admin only)")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all users", description = "Returns a list of all registered users.")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() {
        log.info("REST request to get all users");
        List<UserResponseDto> users = userService.getAllUsers().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PostMapping("/{userId}/status")
    @Operation(summary = "Set user activation status", description = "Activates or deactivates a user account. Triggers email on activation.")
    public ResponseEntity<ApiResponse<UserResponseDto>> setUserStatus(
            @PathVariable Long userId,
            @RequestParam boolean active) {
        log.info("REST request to set status for user {} to {}", userId, active);
        User user = userService.setUserStatus(userId, active);
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", mapToDto(user)));
    }

    private UserResponseDto mapToDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
