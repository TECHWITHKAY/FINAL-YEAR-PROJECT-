package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.request.AuthRequestDto;
import com.ghana.commoditymonitor.dto.request.RegisterRequestDto;
import com.ghana.commoditymonitor.dto.response.JwtResponseDto;
import com.ghana.commoditymonitor.entity.User;
import com.ghana.commoditymonitor.exception.DuplicateResourceException;
import com.ghana.commoditymonitor.repository.UserRepository;
import com.ghana.commoditymonitor.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Authentication and user registration business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    /**
     * Authenticate a user and generate a JWT token.
     */
    public JwtResponseDto login(AuthRequestDto request) {
        log.info("Attempting login for user: {}", request.username());
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("Logged in user not found in database"));

        return JwtResponseDto.builder()
                .token(jwt)
                .username(user.getUsername())
                .role(user.getRole())
                .expiresIn(86400000L) // 24 hours in ms
                .build();
    }

    /**
     * Register a new user (restricted logic should be applied in controller or security config).
     */
    @Transactional
    public JwtResponseDto register(RegisterRequestDto request) {
        log.info("Registering new user: {}", request.username());

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken!");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already in use!");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build();

        userRepository.save(user);

        // Auto-login after registration or just return success? 
        // Request says "authenticate, generate JWT token" for login, but for register it just says "register ADMIN only (check existing user)".
        // Returning a login response for convenience.
        return login(new AuthRequestDto(request.username(), request.password()));
    }
}
