package com.oxiomhub.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for multi-issuer JWT validation (PS-93).
 * Bind via {@code oxiom.security.jwt.issuers} — a comma-separated list of trusted Cognito
 * issuer URLs (one per pool). When set, {@link MultiIssuerJwtDecoder} is auto-configured
 * and validates tokens from any of these issuers.
 */
@ConfigurationProperties(prefix = "oxiom.security.jwt")
public class OxiomJwtProperties {

    private List<String> issuers = List.of();

    public List<String> getIssuers() {
        return issuers;
    }

    public void setIssuers(List<String> issuers) {
        this.issuers = issuers;
    }
}
