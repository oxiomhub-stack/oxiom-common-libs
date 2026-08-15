package com.oxiomhub.common.security;

import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-issuer JWT decoder for Cognito pool separation (PS-93).
 * <p>
 * Inspects the token's {@code iss} claim, validates it against a set of trusted issuers,
 * then delegates to a per-issuer {@link NimbusJwtDecoder} (which caches the JWKS internally).
 * <p>
 * This allows services to accept tokens from multiple Cognito pools (candidate, recruiter,
 * business) without knowing which pool a given request comes from — the token itself carries
 * the issuer, and this decoder routes accordingly.
 */
public final class MultiIssuerJwtDecoder implements JwtDecoder {

    private final Set<String> trustedIssuers;
    private final Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();

    public MultiIssuerJwtDecoder(Set<String> trustedIssuers) {
        if (trustedIssuers == null || trustedIssuers.isEmpty()) {
            throw new IllegalArgumentException("At least one trusted issuer is required");
        }
        this.trustedIssuers = Set.copyOf(trustedIssuers);
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        String issuer = extractIssuer(token);
        if (!trustedIssuers.contains(issuer)) {
            throw new JwtValidationException(
                    "Untrusted issuer: " + issuer,
                    Set.of(new org.springframework.security.oauth2.core.OAuth2Error(
                            "invalid_token", "Untrusted issuer: " + issuer, null))
            );
        }
        return decoderFor(issuer).decode(token);
    }

    private String extractIssuer(String token) {
        try {
            SignedJWT jwt = (SignedJWT) JWTParser.parse(token);
            Object iss = jwt.getJWTClaimsSet().getClaim("iss");
            if (iss == null) {
                throw new JwtException("Token has no issuer claim");
            }
            return iss.toString();
        } catch (ParseException e) {
            throw new JwtException("Invalid JWT: " + e.getMessage(), e);
        }
    }

    private JwtDecoder decoderFor(String issuer) {
        return decoders.computeIfAbsent(issuer, this::createDecoder);
    }

    private JwtDecoder createDecoder(String issuer) {
        String jwksUri = issuer + "/.well-known/jwks.json";
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        decoder.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators
                .createDefaultWithIssuer(issuer));
        return decoder;
    }
}
