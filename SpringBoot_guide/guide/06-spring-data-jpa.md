# 06 - Spring Data JPA

Spring Data JPA simplifies relational persistence by providing repository abstractions on top of JPA. The latest release is 4.0.2.

## When to Use
- You have a relational database and want ORM mapping
- You want derived queries, pagination, and auditing
- You need transactions and a mature persistence ecosystem

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-data-jpa"
```

## Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/app
    username: app
    password: secret
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
```

## Repository Example
```java
@Entity
public class Customer {
    @Id
    private Long id;
    private String firstName;
    private String lastName;
}
```

```java
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByLastName(String lastName);
}
```

## Auditing
```java
@Configuration
@EnableJpaAuditing
class JpaConfig {
}
```

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Order {
    @Id
    private Long id;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
```

## Testing
- Use `@DataJpaTest` for repository-focused tests.
- Use `spring.test.database.replace=none` to keep a real database (often with Testcontainers).

## References
- [Spring Data JPA project page](https://spring.io/projects/spring-data-jpa)
- [Spring Data JPA reference](https://docs.spring.io/spring-data/jpa/reference/)
