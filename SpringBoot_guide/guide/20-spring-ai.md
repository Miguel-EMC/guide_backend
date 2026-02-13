# 20 - Spring AI

Spring AI provides a unified abstraction for chat, embeddings, and AI tooling in Spring applications. The latest release is 1.0.3.

## When to Use
- You want a consistent API across AI providers
- You need chat, embeddings, and vector store integration
- You want Spring Boot auto-configuration for AI clients

## Dependencies
### Maven (OpenAI example)
```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.ai:spring-ai-openai-spring-boot-starter"
```

## Configuration
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

## Chat Example
```java
@RestController
public class AiController {

    private final ChatClient chatClient;

    public AiController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/ai")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }
}
```

## Notes
- Use provider-specific starters for OpenAI, Anthropic, Azure OpenAI, and others.
- Configure timeouts and retries for production workloads.

## References
- [Spring AI project page](https://spring.io/projects/spring-ai)
- [Spring AI reference](https://docs.spring.io/spring-ai/reference/)
