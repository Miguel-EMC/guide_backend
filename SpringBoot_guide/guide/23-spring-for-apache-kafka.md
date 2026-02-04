# 23 - Spring for Apache Kafka

Spring for Apache Kafka is a project that applies core Spring concepts to the development of Kafka-based messaging solutions. It provides a "template" as a high-level abstraction for sending and receiving messages.

To use Spring for Apache Kafka, you will need to add the `spring-kafka` dependency to your project.

Once you have added the dependency, you can inject a `KafkaTemplate` bean into your application and use it to send and receive messages.

Here is an example of how to use Spring for Apache Kafka to send a message to a Kafka topic:

```java
@RestController
public class MyController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public MyController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/messages")
    public void sendMessage(@RequestBody String message) {
        this.kafkaTemplate.send("my-topic", message);
    }
}
```
