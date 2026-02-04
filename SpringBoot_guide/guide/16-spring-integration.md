# 16 - Spring Integration

Spring Integration is a framework that provides an extension of the Spring programming model to support the well-known Enterprise Integration Patterns. It enables lightweight messaging within Spring-based applications and supports integration with external systems via declarative adapters.

To use Spring Integration, you will need to add the `spring-boot-starter-integration` dependency to your project.

Once you have added the dependency, you can create an integration flow by creating a `IntegrationFlow` bean.

Here is an example of a simple integration flow that reads a file from the file system, transforms it, and then writes it to another file:

```java
@Configuration
public class IntegrationConfig {

    @Bean
    public IntegrationFlow fileIntegrationFlow() {
        return IntegrationFlows.from(new FileInboundChannelAdapter(new File("/tmp/input")), e -> e.poller(Pollers.fixedDelay(1000)))
                .transform(new FileToStringTransformer())
                .transform(String.class, String::toUpperCase)
                .handle(new FileWritingMessageHandler(new File("/tmp/output")))
                .get();
    }
}
```
