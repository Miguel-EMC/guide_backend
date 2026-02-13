# 25 - Spring for Apache Pulsar

Spring for Apache Pulsar provides messaging abstractions for Apache Pulsar. The latest release is 1.2.3.

## When to Use
- Event streaming with Pulsar topics
- Multi-tenant messaging
- Pulsar schema support and message routing

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.pulsar</groupId>
  <artifactId>spring-pulsar-spring-boot-starter</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.pulsar:spring-pulsar-spring-boot-starter"
```

## Configuration
```yaml
spring:
  pulsar:
    client:
      service-url: pulsar://localhost:6650
```

## Producing Messages
```java
@Service
public class PulsarPublisher {
    private final PulsarTemplate<String> pulsarTemplate;

    public PulsarPublisher(PulsarTemplate<String> pulsarTemplate) {
        this.pulsarTemplate = pulsarTemplate;
    }

    public void publish(String topic, String payload) {
        pulsarTemplate.send(topic, payload);
    }
}
```

## Consuming Messages
```java
@Service
public class PulsarConsumer {

    @PulsarListener(topics = "orders")
    public void onMessage(String payload) {
        // handle payload
    }
}
```

## References
- [Spring for Apache Pulsar project page](https://spring.io/projects/spring-pulsar)
- [Spring for Apache Pulsar reference](https://docs.spring.io/spring-pulsar/reference/)
