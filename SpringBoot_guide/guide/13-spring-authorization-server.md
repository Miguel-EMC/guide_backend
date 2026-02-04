# 13 - Spring Authorization Server

Spring Authorization Server is a framework that provides implementations of the OAuth 2.1 and OpenID Connect 1.0 specifications. It is built on top of Spring Security and provides a secure and extensible way to build your own authorization server.

To use Spring Authorization Server, you will need to add the `spring-boot-starter-oauth2-authorization-server` dependency to your project.

Once you have added the dependency, you can configure your authorization server by creating a `RegisteredClientRepository` bean and a `JWKSource` bean.

Here is an example of a simple authorization server configuration:

```java
@Configuration
public class AuthorizationServerConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("my-client")
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/my-client-oidc")
                .scope(OidcScopes.OPENID)
                .scope("read")
                .scope("write")
                .build();
        return new InMemoryRegisteredClientRepository(registeredClient);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // ...
    }
}
```
