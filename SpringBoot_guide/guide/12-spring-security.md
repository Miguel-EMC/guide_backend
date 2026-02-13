# 12 - Spring Security

Spring Security is the standard framework for authentication and authorization in Spring applications. The latest release is 7.0.2.

## When to Use
- Securing web applications and APIs
- Authentication with form login, OAuth2, or SSO
- Method-level authorization

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-security"
```

## Basic Security Configuration
```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

## Common Add-ons
- `oauth2Login()` for OAuth2 client login
- `oauth2ResourceServer()` for JWT or opaque token validation
- `csrf()` configuration for browser-based apps

## References
- [Spring Security project page](https://spring.io/projects/spring-security)
- [Spring Security reference](https://docs.spring.io/spring-security/reference/)
