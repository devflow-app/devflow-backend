package com.devflow.auth.service;

import com.devflow.auth.dto.request.OAuth2LoginRequest;
import com.devflow.auth.entity.User;
import com.devflow.auth.repository.UserRepository;
import com.devflow.common.enums.UserRole;
import com.devflow.common.exception.DevFlowException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final UserRepository userRepository;
    private final RestClient restClient = RestClient.create();

    @Transactional
    public User getOrCreateUserFromOAuth(OAuth2LoginRequest request) {
        return switch (request.getProvider().toLowerCase()) {
            case "google" -> handleGoogle(request.getToken());
            case "github" -> handleGithub(request.getToken());
            default -> throw new DevFlowException(
                    "Unsupported OAuth provider: " + request.getProvider(), HttpStatus.BAD_REQUEST);
        };
    }

    @SuppressWarnings("unchecked")
    private User handleGoogle(String idToken) {
        Map<String, Object> info = restClient.get()
                .uri("https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + idToken)
                .retrieve()
                .body(Map.class);

        if (info == null || info.get("sub") == null) {
            throw new DevFlowException("Invalid Google token", HttpStatus.UNAUTHORIZED);
        }

        return findOrCreate(
                "google",
                (String) info.get("sub"),
                (String) info.get("email"),
                (String) info.getOrDefault("given_name", "User"),
                (String) info.getOrDefault("family_name", ""),
                (String) info.get("picture")
        );
    }

    @SuppressWarnings("unchecked")
    private User handleGithub(String accessToken) {
        Map<String, Object> info = restClient.get()
                .uri("https://api.github.com/user")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .body(Map.class);

        if (info == null || info.get("id") == null) {
            throw new DevFlowException("Invalid GitHub token", HttpStatus.UNAUTHORIZED);
        }

        String name = (String) info.getOrDefault("name", "GitHub User");
        String[] parts = name.split(" ", 2);

        return findOrCreate(
                "github",
                String.valueOf(info.get("id")),
                (String) info.get("email"),
                parts[0],
                parts.length > 1 ? parts[1] : "",
                (String) info.get("avatar_url")
        );
    }

    private User findOrCreate(String provider, String providerId, String email,
                               String firstName, String lastName, String avatarUrl) {
        return userRepository
                .findByOauthProviderAndOauthProviderIdAndDeletedAtIsNull(provider, providerId)
                .orElseGet(() -> {
                    if (email != null) {
                        var existing = userRepository.findByEmailAndDeletedAtIsNull(email);
                        if (existing.isPresent()) {
                            User u = existing.get();
                            u.setOauthProvider(provider);
                            u.setOauthProviderId(providerId);
                            if (u.getAvatarUrl() == null) u.setAvatarUrl(avatarUrl);
                            return userRepository.save(u);
                        }
                    }
                    return userRepository.save(User.builder()
                            .email(email != null ? email : provider + "_" + providerId + "@devflow.app")
                            .firstName(firstName)
                            .lastName(lastName)
                            .avatarUrl(avatarUrl)
                            .oauthProvider(provider)
                            .oauthProviderId(providerId)
                            .role(UserRole.MEMBER)
                            .emailVerified(true)
                            .build());
                });
    }
}
