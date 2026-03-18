package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.entity.PasswordResetToken;
import com.ghana.commoditymonitor.entity.User;
import com.ghana.commoditymonitor.exception.BusinessRuleException;
import com.ghana.commoditymonitor.repository.PasswordResetTokenRepository;
import com.ghana.commoditymonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.reset-token.expiry-hours:1}")
    private int expiryHours;

    /**
     * Step 1 — User requests reset.
     * ALWAYS returns without error regardless of whether the email exists.
     * This prevents email enumeration attacks.
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Silently return — caller always gets the same success response
            log.debug("Password reset requested for unknown email: {}", email);
            return;
        }

        User user = userOpt.get();

        // Invalidate any prior unused tokens for this user
        tokenRepository.deleteAllByUserId(user.getId());

        // Generate a cryptographically random 32-byte token, hex-encoded = 64 chars
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);

        // Store only the SHA-256 hash — raw token travels only in the email link
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setTokenHash(hashToken(rawToken));
        prt.setExpiresAt(LocalDateTime.now().plusHours(expiryHours));
        tokenRepository.save(prt);

        // Fire email asynchronously — never blocks this method
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), rawToken);

        log.info("Password reset initiated for user: {}", user.getUsername());
    }

    /**
     * Step 2 — Frontend validates the token before showing the reset form.
     * Returns true if valid, false if expired or not found.
     */
    @Transactional(readOnly = true)
    public boolean isTokenValid(String rawToken) {
        return tokenRepository.findByTokenHash(hashToken(rawToken))
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }

    /**
     * Step 3 — Execute the password reset.
     * Throws BusinessRuleException (HTTP 422) on any failure.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessRuleException("Passwords do not match.");
        }

        PasswordResetToken prt = tokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new BusinessRuleException(
                        "Invalid or expired reset link. Please request a new one."));

        if (prt.isExpired()) {
            throw new BusinessRuleException("This reset link has expired. Please request a new one.");
        }

        if (prt.isUsed()) {
            throw new BusinessRuleException("This reset link has already been used. Please request a new one.");
        }

        // Update password
        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used — do not delete, keep for audit trail
        prt.setUsed(true);
        tokenRepository.save(prt);

        log.info("Password successfully reset for user: {}", user.getUsername());
    }

    /**
     * Called by scheduler nightly to clean up old expired tokens.
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteAllByExpiresAtBefore(LocalDateTime.now().minusHours(24));
        log.debug("Expired password reset tokens cleaned up.");
    }

    // SHA-256 hex hash of a raw string — deterministic, one-way
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
