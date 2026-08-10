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
 * Cognito swap = read {@code cognito:groups} instead. M2M tokens with {@code .../service} scope
 * get {@code ROLE_SERVICE} for internal service-to-service calls (PS-73).
 */
public final class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Set<String> APP_ROLES = Set.of("CANDIDATE", "RECRUITER", "ADMIN");
    private static final List<String> DEFAULT_ROLES = List.of("CANDIDATE");
    private static final String SERVICE_SCOPE_SUFFIX = "/service";

    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));

        // M2M tokens have no cognito:groups but carry a .../service scope.
        if (hasServiceScope(jwt)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SERVICE"));
        } else {
            for (String role : appRoles(jwt)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private static boolean hasServiceScope(Jwt jwt) {
        List<String> scopeList = jwt.getClaimAsStringList("scope");
        if (scopeList != null) {
            return scopeList.stream().anyMatch(s -> s.endsWith(SERVICE_SCOPE_SUFFIX));
        }
        String scopeString = jwt.getClaimAsString("scope");
        if (scopeString != null) {
            for (String s : scopeString.split(" ")) {
                if (s.endsWith(SERVICE_SCOPE_SUFFIX)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * App roles carried by the token, upper-cased and filtered; defaults to {@code [CANDIDATE]}.
     * Reads Cognito's {@code cognito:groups} (cloud) first, then Keycloak's
     * {@code realm_access.roles} (local) — so one converter works for both providers.
     */
    public static List<String> appRoles(Jwt jwt) {
        List<String> fromCognito = mapRoles(jwt.getClaimAsStringList("cognito:groups"));
        if (!fromCognito.isEmpty()) {
            return fromCognito;
        }
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            List<String> fromKeycloak = mapRoles(roles);
            if (!fromKeycloak.isEmpty()) {
                return fromKeycloak;
            }
        }
        return DEFAULT_ROLES;
    }

    /** Upper-case, filter to known app roles, de-duplicate. */
    private static List<String> mapRoles(Collection<?> raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> mapped = new ArrayList<>();
        for (Object role : raw) {
            String upper = String.valueOf(role).toUpperCase(Locale.ROOT);
            if (APP_ROLES.contains(upper) && !mapped.contains(upper)) {
                mapped.add(upper);
            }
        }
        return mapped;
    }
}
