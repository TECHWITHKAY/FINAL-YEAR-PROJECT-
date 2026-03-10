package com.ghana.commoditymonitor.util;

import com.ghana.commoditymonitor.entity.User;
import com.ghana.commoditymonitor.enums.Role;
import com.ghana.commoditymonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Utility class to seed initial data on application startup.
 * Specifically handles creating the initial administrator user if one doesn't exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        seedAdminUser();
    }

    private void seedAdminUser() {
        if (userRepository.findByUsername(adminUsername).isEmpty() && 
            userRepository.findByEmail(adminEmail).isEmpty()) {
            
            log.info("No admin user found. Creating default admin: {}", adminUsername);
            
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            
            userRepository.save(admin);
            log.info("Default admin user created successfully.");
        } else {
            log.info("Admin user already exists. Skipping dynamic seeding.");
        }
    }
}
