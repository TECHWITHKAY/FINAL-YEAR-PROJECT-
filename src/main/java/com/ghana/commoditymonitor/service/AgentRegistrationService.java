package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.request.RegisterAgentRequestDto;
import com.ghana.commoditymonitor.entity.User;
import com.ghana.commoditymonitor.exception.BusinessRuleException;
import com.ghana.commoditymonitor.exception.ResourceNotFoundException;
import com.ghana.commoditymonitor.enums.Role;
import com.ghana.commoditymonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.admin.email:admin@commoditygh.com}")
    private String adminEmail;

    /**
     * Public self-registration for field agents.
     * Account is created with active=false — cannot log in until admin approves.
     */
    @Transactional
    public void registerAgent(RegisterAgentRequestDto request) {
        // Check username uniqueness
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessRuleException("Username '" + request.getUsername() + "' is already taken.");
        }
        // Check email uniqueness — look up by email using existing repo method
        // Use whichever method name UserRepository already has for email lookup
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessRuleException("An account already exists for this email address.");
        }

        User agent = new User();
        agent.setUsername(request.getUsername());
        agent.setEmail(request.getEmail());
        agent.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        agent.setRole(Role.FIELD_AGENT);
        agent.setActive(false); // blocked until admin approves
        agent.setOperatingCity(request.getOperatingCity());
        agent.setApplicationNote(request.getApplicationNote());

        userRepository.save(agent);
        log.info("Field agent application submitted: {}", request.getUsername());

        // Both emails are async and fire-and-forget
        emailService.sendAgentApplicationReceivedEmail(agent.getEmail(), agent.getUsername());
        emailService.sendAgentApplicationAdminNotifyEmail(
            adminEmail,
            agent.getUsername(),
            agent.getEmail(),
            request.getOperatingCity(),
            request.getApplicationNote()
        );
    }

    /**
     * Admin approves a pending field agent — sets active=true and sends approval email.
     */
    @Transactional
    public void approveAgent(Long userId) {
        User agent = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (Role.FIELD_AGENT != agent.getRole()) {
            throw new BusinessRuleException("This user is not a field agent application.");
        }
        if (agent.isActive()) {
            throw new BusinessRuleException("This agent account is already active.");
        }

        agent.setActive(true);
        userRepository.save(agent);
        log.info("Field agent approved: {}", agent.getUsername());

        emailService.sendAgentApprovedEmail(agent.getEmail(), agent.getUsername());
    }

    /**
     * Admin rejects a pending field agent — sends rejection email.
     * Sets role back to ANALYST so they can still use the platform as a regular user.
     */
    @Transactional
    public void rejectAgent(Long userId, String reason) {
        User agent = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (Role.FIELD_AGENT != agent.getRole() || agent.isActive()) {
            throw new BusinessRuleException("No pending agent application found for this user.");
        }

        String email = agent.getEmail();
        String username = agent.getUsername();

        // Downgrade role — they can re-register as a regular ANALYST if they wish
        agent.setRole(Role.ANALYST);
        agent.setActive(false);
        userRepository.save(agent);
        log.info("Field agent application rejected: {}", username);

        emailService.sendAgentRejectedEmail(email, username, reason);
    }

    /**
     * Returns all users with role=FIELD_AGENT and active=false (pending review).
     */
    @Transactional(readOnly = true)
    public List<User> getPendingAgentApplications() {
        return userRepository.findByRoleAndActiveFalse("FIELD_AGENT");
    }
}
