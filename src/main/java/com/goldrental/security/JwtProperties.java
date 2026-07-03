package com.goldrental.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly typed binding for JWT configuration.
 * Values are loaded from application.yml (app.jwt.*).
 *
 * <p>Guideline: NoHardcodedSecrets — all secrets come from environment variables.
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {
    /** HS256 signing secret — must be at least 256 bits (32 chars). */
    private String secret;
    /** Access token TTL in milliseconds. */
    private long expirationMs;
    /** Refresh token TTL in milliseconds. */
    private long refreshExpirationMs;
}
