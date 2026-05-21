package com.devflow.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "devflow.jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret;
    private long expiration = 86400000L;         // 24h default
    private long refreshExpiration = 604800000L; // 7d default
}
