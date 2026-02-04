# 22 - Spring AMQP

Spring AMQP is a project that applies core Spring concepts to the development of AMQP-based messaging solutions. It provides a "template" as a high-level abstraction for sending and receiving messages.

To use Spring AMQP, you will need to add the `spring-boot-starter-amqp` dependency to your project.

Once you have added the dependency, you can inject a `RabbitTemplate` bean into your application and use it to send and receive messages.

Here is an example of how to use Spring AMQP to send a message to a RabbitMQ queue:

```java
@RestController
public class MyController {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public MyController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/messages")
    public void sendMessage(@RequestBody String message) {
        this.rabbitTemplate.convertAndSend("my-queue", message);
    }
}
```
