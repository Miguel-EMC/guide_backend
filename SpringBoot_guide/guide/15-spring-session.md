# 15 - Spring Session

Spring Session provides an API and implementations for managing user sessions in a container-neutral way. The latest release is 4.0.1.

## When to Use
- You need clustered sessions across multiple instances
- You want to externalize session storage
- You need consistent session behavior across servlet containers

## Dependencies
Common stores include Redis and JDBC.

### Maven (Redis)
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.session</groupId>
  <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

### Gradle (JDBC)
```gradle
implementation "org.springframework.session:spring-session-jdbc"
implementation "org.springframework.boot:spring-boot-starter-jdbc"
```

## Configuration (Redis)
```yaml
spring:
  session:
    store-type: redis
  data:
    redis:
      host: localhost
      port: 6379
```

## Configuration (JDBC)
```yaml
spring:
  session:
    store-type: jdbc
    jdbc:
      initialize-schema: always
```

## Enabling with Annotations
You can enable a store explicitly if you are not relying on Spring Boot auto-configuration.

```java
@Configuration
@EnableRedisHttpSession
public class SessionConfig {
}
```

## Notes
- Redis is the most common production store.
- JDBC is useful when Redis is not available.

## References
- [Spring Session project page](https://spring.io/projects/spring-session)
- [Spring Session reference](https://docs.spring.io/spring-session/reference/)
