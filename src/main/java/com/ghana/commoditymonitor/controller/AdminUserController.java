package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.dto.response.ApiResponse;
import com.ghana.commoditymonitor.dto.response.UserDto;
import com.ghana.commoditymonitor.entity.User;
import com.ghana.commoditymonitor.service.AgentRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "User management for administrators")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AgentRegistrationService agentService;

    @GetMapping("/pending-agents")
    @Operation(summary = "List all field agents pending approval")
    public ResponseEntity<ApiResponse<List<UserDto>>> getPendingAgents() {
        List<UserDto> dtos = agentService.getPendingAgentApplications().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Pending agents retrieved", dtos));
    }

    @PostMapping("/{id}/approve-agent")
    @Operation(summary = "Approve a field agent application")
    public ResponseEntity<ApiResponse<Void>> approveAgent(@PathVariable Long id) {
        agentService.approveAgent(id);
        return ResponseEntity.ok(ApiResponse.ok("Agent application approved.", null));
    }

    @PostMapping("/{id}/reject-agent")
    @Operation(summary = "Reject a field agent application")
    public ResponseEntity<ApiResponse<Void>> rejectAgent(@PathVariable Long id, @RequestParam(required = false) String reason) {
        agentService.rejectAgent(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Agent application rejected.", null));
    }

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.isActive())
                .operatingCity(user.getOperatingCity())
                .applicationNote(user.getApplicationNote())
                .build();
    }
}
