package com.ghana.commoditymonitor.dto.response;

import com.ghana.commoditymonitor.enums.Role;
import lombok.*;

/**
 * Response DTO for JWT authentication results.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDto {
    private String token;
    private String username;
    private Role role;
    private Long expiresIn;
}
