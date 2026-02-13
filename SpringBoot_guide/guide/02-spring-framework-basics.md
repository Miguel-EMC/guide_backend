# 02 - Spring Framework Basics

The Spring Framework provides the core programming model for building Java applications. The latest major line is Spring Framework 7.0.4.

## Core Modules
- **Core Container:** IoC container, bean lifecycle, and dependency injection
- **Spring Beans:** `@Component`, `@Configuration`, and `@Bean`
- **Spring Context:** Application events, resource loading, and internationalization
- **AOP:** Cross-cutting concerns like logging and transactions

## The IoC Container
The IoC container manages bean creation, wiring, and lifecycle. The two main container types are:
- `BeanFactory`: minimal features and lazy initialization
- `ApplicationContext`: full feature set with events and resource loading

## Defining Beans
```java
@Configuration
public class AppConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
```

## Bean Scopes
Common scopes include:
- `singleton` (default)
- `prototype`
- `request`, `session`, `application` (web-aware)

## Application Events
```java
@Component
public class StartupListener {

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        // app is ready
    }
}
```

## References
- [Spring Framework project page](https://spring.io/projects/spring-framework)
- [Spring Framework reference](https://docs.spring.io/spring-framework/reference/)
