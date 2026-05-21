package com.devflow.auth.service;

import com.devflow.auth.config.JwtProperties;
import com.devflow.auth.entity.RefreshToken;
import com.devflow.auth.entity.User;
import com.devflow.auth.repository.RefreshTokenRepository;
import com.devflow.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpiration()))
                .build();
        return refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or revoked refresh token"));

        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token expired. Please log in again");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Transactional
    public void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }
}
