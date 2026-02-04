# 12 - Spring Security

Spring Security is a powerful and highly customizable authentication and access-control framework. It is the de-facto standard for securing Spring-based applications.

To use Spring Security, you will need to add the `spring-boot-starter-security` dependency to your project.

Once you have added the dependency, Spring Security will automatically secure your application. By default, it will require you to provide a username and password to access your application.

You can customize the security configuration of your application by creating a `SecurityFilterChain` bean.

Here is an example of a simple security configuration:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .formLogin(withDefaults());
        return http.build();
    }
}
```
