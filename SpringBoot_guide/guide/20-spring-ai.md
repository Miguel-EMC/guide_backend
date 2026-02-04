# 20 - Spring AI

Spring AI is an artificial intelligence framework for Spring applications. It provides a set of APIs and implementations for interacting with AI models from various providers, such as OpenAI and Hugging Face.

To use Spring AI, you will need to add the `spring-ai-boot-starter` dependency to your project, along with a dependency for the AI model that you want to use (e.g., `spring-ai-openai-starter`).

Once you have added the dependencies, you can inject an `AiClient` bean into your application and use it to interact with the AI model.

Here is an example of how to use Spring AI to generate a response from an AI model:

```java
@RestController
public class MyController {

    private final AiClient aiClient;

    @Autowired
    public MyController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @GetMapping("/ai")
    public String ai(@RequestParam String message) {
        return this.aiClient.generate(message);
    }
}
```
