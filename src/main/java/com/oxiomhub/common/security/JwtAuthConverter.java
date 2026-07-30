package com.oxiomhub.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maps a Keycloak/Cognito JWT to Spring authorities: keeps the default {@code SCOPE_*} and adds
 * {@code ROLE_*} for app roles found under {@code realm_access.roles}, defaulting to CANDIDATE.
 * Cognito swap = read {@code cognito:groups} instead.
 */
public final class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Set<String> APP_ROLES = Set.of("CANDIDATE", "RECRUITER", "ADMIN");
    private static final List<String> DEFAULT_ROLES = List.of("CANDIDATE");

    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
        for (String role : appRoles(jwt)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    /** App roles carried by the token, upper-cased and filtered; defaults to {@code [CANDIDATE]}. */
    public static List<String> appRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            List<String> mapped = new ArrayList<>();
            for (Object role : roles) {
                String upper = String.valueOf(role).toUpperCase(Locale.ROOT);
                if (APP_ROLES.contains(upper) && !mapped.contains(upper)) {
                    mapped.add(upper);
                }
            }
            if (!mapped.isEmpty()) {
                return List.copyOf(mapped);
            }
        }
        return DEFAULT_ROLES;
    }
}
