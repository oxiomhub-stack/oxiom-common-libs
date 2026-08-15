package com.oxiomhub.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for multi-issuer JWT validation (PS-93).
 * <p>
 * When {@code oxiom.security.jwt.issuers} is set (comma-separated list of Cognito issuer URIs),
 * the library auto-registers a {@link MultiIssuerJwtDecoder} that trusts all listed issuers
 * and routes each token to the correct JWKS based on its {@code iss} claim.
 * <p>
 * Example (application-cloud.yml):
 * <pre>
 * oxiom:
 *   security:
 *     jwt:
 *       issuers:
 *         - https://cognito-idp.eu-west-2.amazonaws.com/eu-west-2_candidate
 *         - https://cognito-idp.eu-west-2.amazonaws.com/eu-west-2_recruiter
 *         - https://cognito-idp.eu-west-2.amazonaws.com/eu-west-2_business
 * </pre>
 */
@ConfigurationProperties(prefix = "oxiom.security.jwt")
public class MultiIssuerJwtProperties {

    /**
     * List of trusted JWT issuer URIs. Tokens with an {@code iss} claim not in this list
     * are rejected. Each issuer's JWKS is fetched from {@code {issuer}/.well-known/jwks.json}.
     */
    private List<String> issuers = List.of();

    public List<String> getIssuers() {
        return issuers;
    }

    public void setIssuers(List<String> issuers) {
        this.issuers = issuers;
    }
}
