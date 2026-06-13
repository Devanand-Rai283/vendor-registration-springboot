package com.streetvendor.auth.repository;

import com.streetvendor.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    List<RefreshToken> findByUserId(UUID userId);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);

    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
