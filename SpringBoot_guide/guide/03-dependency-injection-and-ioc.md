# 03 - Dependency Injection and IoC

Dependency Injection (DI) is how Spring implements Inversion of Control (IoC). Instead of creating dependencies manually, the container supplies them.

## Injection Styles
- **Constructor injection** (recommended)
- **Setter injection** (optional dependencies)
- **Field injection** (discouraged for testability)

## Constructor Injection Example
```java
@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

## Qualifiers and Primary Beans
```java
@Service
@Primary
public class PrimaryNotifier implements Notifier {
}

@Service
@Qualifier("sms")
public class SmsNotifier implements Notifier {
}
```

```java
public class AlertService {
    private final Notifier notifier;

    public AlertService(@Qualifier("sms") Notifier notifier) {
        this.notifier = notifier;
    }
}
```

## Profiles
```java
@Configuration
@Profile("dev")
class DevOnlyConfig {
}
```

## Notes
- Prefer constructor injection for immutability and testability.
- Use `ObjectProvider<T>` for optional or lazy dependencies.

## References
- [Spring Framework reference](https://docs.spring.io/spring-framework/reference/)
