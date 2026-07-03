package com.goldrental.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Generates and validates JWT access tokens.
 *
 * <p>Security considerations:
 * <ul>
 *   <li>Uses HMAC-SHA256 (HS256) — secret is loaded from environment, never hardcoded.</li>
 *   <li>Token expiry is always validated.</li>
 *   <li>All parse exceptions are caught and logged without leaking internals.</li>
 * </ul>
 *
 * <p>Guideline: NoHardcodedSecrets — secret injected via {@link JwtProperties}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    /**
     * Generates an access token for an authenticated user.
     *
     * @param authentication the Spring Security authentication object
     * @return signed JWT string
     */
    public String generateToken(final Authentication authentication) {
        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return buildToken(userDetails.getUsername(), jwtProperties.getExpirationMs());
    }

    /**
     * Generates a token directly from an email (used post-registration).
     */
    public String generateTokenFromEmail(final String email) {
        return buildToken(email, jwtProperties.getExpirationMs());
    }

    private String buildToken(final String subject, final long ttlMs) {
        final Date now = new Date();
        final Date expiry = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the email (subject) from a validated token.
     */
    public String getEmailFromToken(final String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Returns the configured TTL in milliseconds (used in AuthResponse).
     */
    public long getExpirationMs() {
        return jwtProperties.getExpirationMs();
    }

    /**
     * Validates the token: signature, expiry, and structure.
     *
     * @param token the raw JWT string
     * @return true if valid, false otherwise (specific error logged at WARN)
     */
    public boolean validateToken(final String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT: {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Empty JWT claims: {}", ex.getMessage());
        }
        return false;
    }

    // ─── Private helpers ───────────────────────────────────────

    private Claims parseClaims(final String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        final byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(
                        jwtProperties.getSecret().getBytes()
                )
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
