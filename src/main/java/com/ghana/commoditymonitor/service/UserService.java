package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.entity.User;
import com.ghana.commoditymonitor.exception.ResourceNotFoundException;
import com.ghana.commoditymonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service to manage user accounts and status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * List all users in the system.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Activate or Deactivate a user account.
     * If activated, trigger an email notification.
     */
    @Transactional
    public User setUserStatus(Long userId, boolean active) {
        log.info("Setting status for user id {}: {}", userId, active ? "ACTIVE" : "INACTIVE");
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        boolean previouslyInactive = !user.isActive();
        user.setActive(active);
        
        User savedUser = userRepository.save(user);
        
        // If account was just activated, send the notification
        if (active && previouslyInactive) {
            emailService.sendAccountActivatedEmail(user.getEmail(), user.getUsername());
        }
        
        return savedUser;
    }
}
