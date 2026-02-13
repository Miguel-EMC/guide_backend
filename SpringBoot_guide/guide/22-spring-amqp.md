# 22 - Spring AMQP

Spring AMQP provides messaging abstractions for AMQP brokers such as RabbitMQ. The latest release is 4.0.1.

## When to Use
- Asynchronous messaging with RabbitMQ
- Event-driven architectures
- Task queues and pub/sub

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-amqp"
```

## Configuration
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

## Sending Messages
```java
@Service
public class OrderPublisher {
    private final RabbitTemplate rabbitTemplate;

    public OrderPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(String queue, String payload) {
        rabbitTemplate.convertAndSend(queue, payload);
    }
}
```

## Listening for Messages
```java
@Service
public class OrderConsumer {

    @RabbitListener(queues = "orders")
    public void onMessage(String payload) {
        // handle payload
    }
}
```

## References
- [Spring AMQP project page](https://spring.io/projects/spring-amqp)
- [Spring AMQP reference](https://docs.spring.io/spring-amqp/reference/)
