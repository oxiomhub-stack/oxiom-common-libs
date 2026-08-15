package com.oxiomhub.common.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link JwtDecoder} that validates tokens from multiple Cognito user pools (PS-93).
 * On first decode, extracts the {@code iss} claim from the token, verifies it's in the
 * trusted set, then lazily creates and caches a standard issuer-specific decoder for it.
 * Tokens from untrusted issuers are rejected immediately.
 */
public final class MultiIssuerJwtDecoder implements JwtDecoder {

    private final Map<String, String> trustedIssuers;
    private final Map<String, JwtDecoder> decoderCache = new ConcurrentHashMap<>();

    public MultiIssuerJwtDecoder(Iterable<String> issuerUris) {
        Map<String, String> map = new ConcurrentHashMap<>();
        for (String uri : issuerUris) {
            String normalized = uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
            map.put(normalized, normalized);
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException("At least one trusted issuer URI is required");
        }
        this.trustedIssuers = map;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        String issuer = extractIssuer(token);
        if (issuer == null) {
            throw new JwtException("Token has no iss claim");
        }
        String normalized = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        if (!trustedIssuers.containsKey(normalized)) {
            throw new JwtException("Untrusted issuer: " + issuer);
        }
        JwtDecoder decoder = decoderCache.computeIfAbsent(normalized, JwtDecoders::fromIssuerLocation);
        return decoder.decode(token);
    }

    private static String extractIssuer(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            int issIdx = payload.indexOf("\"iss\"");
            if (issIdx < 0) {
                return null;
            }
            int colonIdx = payload.indexOf(':', issIdx);
            int startQuote = payload.indexOf('"', colonIdx);
            int endQuote = payload.indexOf('"', startQuote + 1);
            if (startQuote < 0 || endQuote < 0) {
                return null;
            }
            return payload.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            return null;
        }
    }
}
