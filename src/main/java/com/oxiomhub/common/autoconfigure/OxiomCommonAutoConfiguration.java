package com.oxiomhub.common.autoconfigure;

import com.oxiomhub.common.event.DomainEventPublisher;
import com.oxiomhub.common.event.NoOpDomainEventPublisher;
import com.oxiomhub.common.security.MultiIssuerJwtDecoder;
import com.oxiomhub.common.security.OxiomJwtProperties;
import com.oxiomhub.common.web.ApiExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Auto-registers the shared OxiomHub beans in any consuming service:
 * the RFC-7807 error handler, the no-op domain-event publisher, CORS source for the SPA,
 * and (PS-93) a multi-issuer JwtDecoder when {@code oxiom.security.jwt.issuers} is set.
 * Each is {@code @ConditionalOnMissingBean}, so a service can override any of them.
 */
@AutoConfiguration(before = OAuth2ResourceServerAutoConfiguration.class)
@EnableConfigurationProperties(OxiomJwtProperties.class)
public class OxiomCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApiExceptionHandler apiExceptionHandler() {
        return new ApiExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public DomainEventPublisher domainEventPublisher() {
        return new NoOpDomainEventPublisher();
    }

    /**
     * CORS for the SPA (bearer-token auth, so no credentials). Named {@code corsConfigurationSource}
     * so {@code http.cors(withDefaults())} picks it up. Origins from {@code app.cors.allowed-origins}.
     */
    @Bean
    @ConditionalOnMissingBean(name = "corsConfigurationSource")
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Multi-issuer JWT decoder for validating tokens from multiple Cognito pools (PS-93).
     * Only activates when {@code oxiom.security.jwt.issuers} is set — local dev (Keycloak)
     * continues to use the standard single-issuer decoder configured by Spring Boot.
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @ConditionalOnProperty(prefix = "oxiom.security.jwt", name = "issuers[0]")
    public JwtDecoder jwtDecoder(OxiomJwtProperties props) {
        return new MultiIssuerJwtDecoder(props.getIssuers());
    }
}
