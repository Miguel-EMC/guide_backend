# 10 - Spring Data R2DBC

Spring Data R2DBC provides reactive repository support for relational databases using R2DBC drivers. The latest release is 4.0.2.

## When to Use
- You need reactive, non-blocking SQL access
- You want backpressure-aware data pipelines
- Your drivers support R2DBC

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-data-r2dbc"
```

You also need a driver, for example:
```gradle
implementation "io.r2dbc:r2dbc-postgresql"
```

## Configuration
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/app
    username: app
    password: secret
```

## Repository Example
```java
@Table("customers")
public class Customer {
    @Id
    private Long id;
    private String firstName;
    private String lastName;
}
```

```java
public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {
    Flux<Customer> findByLastName(String lastName);
}
```

## Transactions
Use `R2dbcTransactionManager` with `@Transactional` for reactive transactions.

## References
- [Spring Data R2DBC project page](https://spring.io/projects/spring-data-r2dbc)
- [Spring Data R2DBC reference](https://docs.spring.io/spring-data/r2dbc/reference/)
