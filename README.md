# oxiom-common-libs

Shared building blocks for OxiomHub Spring Boot services (PS-13). Published to GitHub Packages.

## What's inside
- `com.oxiomhub.common.security.JwtAuthConverter` — Keycloak/Cognito realm roles → `ROLE_*`.
- `com.oxiomhub.common.web.ApiExceptionHandler` — RFC-7807 `problem+json` (400/403/404/409/500).
- `com.oxiomhub.common.event.DomainEventPublisher` (+ `NoOpDomainEventPublisher`).
- CORS source for the SPA origin.

These are auto-registered via `OxiomCommonAutoConfiguration` (each `@ConditionalOnMissingBean`, so a service can override any of them). `JwtAuthConverter` is a plain class used directly in a service's `SecurityConfig`.

## Consume it
Add the repository and dependency:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/oxiomhub-stack/oxiom-common-libs</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.oxiomhub</groupId>
  <artifactId>oxiom-common-libs</artifactId>
  <version>0.1.0</version>
</dependency>
```

Both publishing and consuming authenticate via a `github` server in `~/.m2/settings.xml` (CI uses the built-in `GITHUB_TOKEN`).

## Publish
Automated: pushing to `dev` runs `.github/workflows/publish.yml` → `mvn deploy` to GitHub Packages. Bump `<version>` for a new release.
