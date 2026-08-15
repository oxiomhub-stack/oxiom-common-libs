package com.oxiomhub.common.autoconfigure;

import com.oxiomhub.common.security.MultiIssuerJwtDecoder;
import com.oxiomhub.common.security.MultiIssuerJwtProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.HashSet;

/**
 * Auto-configuration for multi-issuer JWT validation (PS-93).
 * <p>
 * When {@code oxiom.security.jwt.issuers[0]} is set, registers a {@link MultiIssuerJwtDecoder}
 * that trusts all configured issuers and routes tokens by their {@code iss} claim.
 * This replaces Spring's default single-issuer decoder.
 * <p>
 * If the property is not set, no bean is created and services fall back to Spring Boot's
 * standard {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} config.
 */
@AutoConfiguration
@ConditionalOnClass(JwtDecoder.class)
@EnableConfigurationProperties(MultiIssuerJwtProperties.class)
public class OxiomSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @ConditionalOnProperty(prefix = "oxiom.security.jwt", name = "issuers[0]")
    public JwtDecoder multiIssuerJwtDecoder(MultiIssuerJwtProperties properties) {
        return new MultiIssuerJwtDecoder(new HashSet<>(properties.getIssuers()));
    }
}
