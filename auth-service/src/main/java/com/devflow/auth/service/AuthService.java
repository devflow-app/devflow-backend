package com.devflow.auth.service;

import com.devflow.auth.config.JwtProperties;
import com.devflow.auth.dto.request.*;
import com.devflow.auth.dto.response.*;
import com.devflow.auth.entity.RefreshToken;
import com.devflow.auth.entity.User;
import com.devflow.auth.kafka.AuthEventProducer;
import com.devflow.auth.repository.UserRepository;
import com.devflow.auth.security.JwtUtils;
import com.devflow.common.enums.UserRole;
import com.devflow.common.event.UserCreatedEvent;
import com.devflow.common.exception.DevFlowException;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final OAuthService oAuthService;
    private final AuthEventProducer eventProducer;

    // ── Register ─────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DevFlowException("Email already registered", HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(UserRole.MEMBER)
                .build();

        user = userRepository.save(user);

        eventProducer.publishUserCreated(UserCreatedEvent.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build());

        log.info("New user registered: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    // ── Login ─────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new DevFlowException("User not found", HttpStatus.NOT_FOUND));

        // 2FA check
        if (user.isTotpEnabled()) {
            if (request.getTotpCode() == null || request.getTotpCode().isBlank()) {
                return AuthResponse.builder().totpRequired(true).tokenType("Bearer").build();
            }
            if (!verifyTotpCode(user.getTotpSecret(), request.getTotpCode())) {
                throw new DevFlowException("Invalid 2FA code", HttpStatus.UNAUTHORIZED, "INVALID_TOTP");
            }
        }

        return buildAuthResponse(user);
    }

    // ── OAuth2 Login ──────────────────────────────────────────

    @Transactional
    public AuthResponse loginWithOAuth(OAuth2LoginRequest request) {
        User user = oAuthService.getOrCreateUserFromOAuth(request);
        return buildAuthResponse(user);
    }

    // ── Refresh Token ─────────────────────────────────────────

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();
        refreshTokenService.revokeToken(refreshToken); // Rotate token
        return buildAuthResponse(user);
    }

    // ── Logout ────────────────────────────────────────────────

    @Transactional
    public void logout(String refreshTokenStr) {
        RefreshToken token = refreshTokenService.validateRefreshToken(refreshTokenStr);
        refreshTokenService.revokeToken(token);
        log.info("User {} logged out", token.getUser().getEmail());
    }

    // ── 2FA Setup ─────────────────────────────────────────────

    @Transactional
    public TotpSetupResponse setupTotp(UUID userId) {
        User user = findUserById(userId);
        String secret = new DefaultSecretGenerator().generate();
        user.setTotpSecret(secret);
        userRepository.save(user);

        QrData qrData = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer("DevFlow")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            var generator = new ZxingPngQrGenerator();
            byte[] imageData = generator.generate(qrData);
            String qrCodeUrl = getDataUriForImage(imageData, generator.getImageMimeType());

            return TotpSetupResponse.builder()
                    .secret(secret)
                    .qrCodeUrl(qrCodeUrl)
                    .otpAuthUrl(qrData.getUri())
                    .build();
        } catch (Exception e) {
            throw new DevFlowException("Failed to generate QR code", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void enableTotp(UUID userId, VerifyTotpRequest request) {
        User user = findUserById(userId);
        validateTotp(user.getTotpSecret(), request.getCode());
        user.setTotpEnabled(true);
        userRepository.save(user);
        log.info("2FA enabled for user: {}", user.getEmail());
    }

    @Transactional
    public void disableTotp(UUID userId, VerifyTotpRequest request) {
        User user = findUserById(userId);
        validateTotp(user.getTotpSecret(), request.getCode());
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        log.info("2FA disabled for user: {}", user.getEmail());
    }

    // ── Helpers ───────────────────────────────────────────────

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .totpEnabled(user.isTotpEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthResponse buildAuthResponse(User user) {
        refreshTokenService.revokeAllUserTokens(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .totpRequired(false)
                .user(mapToUserResponse(user))
                .build();
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DevFlowException("User not found", HttpStatus.NOT_FOUND));
    }

    private boolean verifyTotpCode(String secret, String code) {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(), new SystemTimeProvider());
        return verifier.isValidCode(secret, code);
    }

    private void validateTotp(String secret, String code) {
        if (!verifyTotpCode(secret, code)) {
            throw new DevFlowException("Invalid TOTP code", HttpStatus.UNAUTHORIZED, "INVALID_TOTP");
        }
    }
}
