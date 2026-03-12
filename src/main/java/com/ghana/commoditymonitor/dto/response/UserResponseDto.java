package com.ghana.commoditymonitor.dto.response;

import com.ghana.commoditymonitor.enums.Role;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * Response DTO for user information (safely excludes password).
 */
@Builder
public record UserResponseDto(
    Long id,
    String username,
    String email,
    Role role,
    boolean active,
    OffsetDateTime createdAt
) {}
