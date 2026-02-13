# 13 - Spring Authorization Server

Spring Authorization Server provides OAuth 2.1 and OpenID Connect 1.0 support for building authorization servers. The latest release is 1.5.5.

## When to Use
- You need to issue access and refresh tokens
- You want to implement OAuth2/OIDC flows yourself
- You need a first-party authorization server

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-oauth2-authorization-server"
```

## Minimal Configuration
```java
@Configuration
public class AuthorizationServerConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("web-client")
            .clientSecret("{noop}secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://127.0.0.1:8080/login/oauth2/code/web-client")
            .scope(OidcScopes.OPENID)
            .scope("read")
            .build();
        return new InMemoryRegisteredClientRepository(client);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // Provide a JWK source backed by an RSA or EC key pair.
        return (jwkSelector, context) -> jwkSelector.select(new JWKSet());
    }
}
```

## Notes
- Use a persistent `RegisteredClientRepository` for production.
- Configure JWK rotation and secure key storage.
- The project has moved to the Spring Security umbrella as of 1.5.x.

## References
- [Spring Authorization Server project page](https://spring.io/projects/spring-authorization-server)
- [Spring Authorization Server reference](https://docs.spring.io/spring-authorization-server/reference/)
