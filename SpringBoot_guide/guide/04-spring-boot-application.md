# 04 - The @SpringBootApplication Annotation

The `@SpringBootApplication` annotation is a convenience annotation that adds all of the following:

*   **`@Configuration`:** Tags the class as a source of bean definitions for the application context.
*   **`@EnableAutoConfiguration`:** Tells Spring Boot to start adding beans based on classpath settings, other beans, and various property settings.
*   **`@ComponentScan`:** Tells Spring to look for other components, configurations, and services in the specified package, allowing it to find and register the beans.

Here is an example of a Spring Boot application:

```java
@SpringBootApplication
public class MyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```
