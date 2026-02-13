# 23 - Spring for Apache Kafka

Spring for Apache Kafka provides messaging abstractions for Kafka. The latest release is 3.3.2.

## When to Use
- Event streaming and pub/sub
- High-throughput messaging
- Integration with Kafka consumer groups

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.kafka:spring-kafka"
```

Spring Boot users typically prefer:
```gradle
implementation "org.springframework.boot:spring-boot-starter-kafka"
```

## Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

## Producing Messages
```java
@Service
public class EventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String payload) {
        kafkaTemplate.send(topic, payload);
    }
}
```

## Consuming Messages
```java
@Service
public class EventConsumer {

    @KafkaListener(topics = "events", groupId = "orders")
    public void onMessage(String payload) {
        // handle payload
    }
}
```

## References
- [Spring for Apache Kafka project page](https://spring.io/projects/spring-kafka)
- [Spring for Apache Kafka reference](https://docs.spring.io/spring-kafka/reference/)
