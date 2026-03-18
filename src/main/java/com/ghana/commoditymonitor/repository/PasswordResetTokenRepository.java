package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteAllByUserId(Long userId);

    void deleteAllByExpiresAtBefore(LocalDateTime cutoff);
}
