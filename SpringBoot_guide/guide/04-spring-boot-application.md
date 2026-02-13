# 04 - The @SpringBootApplication Annotation

`@SpringBootApplication` is a convenience annotation that combines:
- `@SpringBootConfiguration`
- `@EnableAutoConfiguration`
- `@ComponentScan`

## Minimal Application
```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## Customizing Startup
```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
            .profiles("prod")
            .logStartupInfo(true)
            .run(args);
    }
}
```

## Command-Line Runners
```java
@Component
public class SeedDataRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        // seed data on startup
    }
}
```

## Configuration Properties
Use `@ConfigurationProperties` and `@ConfigurationPropertiesScan` to bind configuration into typed objects.

## References
- [Spring Boot reference](https://docs.spring.io/spring-boot/reference/)
