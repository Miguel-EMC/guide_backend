# 16 - Spring Integration

Spring Integration extends the Spring programming model to support Enterprise Integration Patterns (EIP). The latest release is 7.0.2.

## When to Use
- You need message-driven integrations between systems
- You want to model flows with channels, transformers, and adapters
- You need reliable polling and routing

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-integration</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-integration"
```

## Integration Flow Example
```java
@Configuration
public class IntegrationConfig {

    @Bean
    public IntegrationFlow fileToLogFlow() {
        return IntegrationFlows
            .from(Files.inboundAdapter(new File("input"))
                .autoCreateDirectory(true),
                e -> e.poller(Pollers.fixedDelay(1000)))
            .transform(Files.toStringTransformer())
            .handle(m -> System.out.println(m.getPayload()))
            .get();
    }
}
```

## Notes
- Prefer `IntegrationFlows` for readable DSL-style configuration.
- Use message channels to decouple producers and consumers.

## References
- [Spring Integration project page](https://spring.io/projects/spring-integration)
- [Spring Integration reference](https://docs.spring.io/spring-integration/reference/)
