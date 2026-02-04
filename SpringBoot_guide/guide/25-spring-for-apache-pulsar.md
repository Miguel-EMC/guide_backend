# 25 - Spring for Apache Pulsar

Spring for Apache Pulsar is a project that applies core Spring concepts to the development of Pulsar-based messaging solutions. It provides a "template" as a high-level abstraction for sending and receiving messages.

To use Spring for Apache Pulsar, you will need to add the `spring-pulsar-spring-boot-starter` dependency to your project.

Once you have added the dependency, you can inject a `PulsarTemplate` bean into your application and use it to send and receive messages.

Here is an example of how to use Spring for Apache Pulsar to send a message to a Pulsar topic:

```java
@RestController
public class MyController {

    private final PulsarTemplate<String> pulsarTemplate;

    @Autowired
    public MyController(PulsarTemplate<String> pulsarTemplate) {
        this.pulsarTemplate = pulsarTemplate;
    }

    @PostMapping("/messages")
    public void sendMessage(@RequestBody String message) {
        this.pulsarTemplate.send("my-topic", message);
    }
}
```
