# 07 - Spring Data JDBC

Spring Data JDBC is a lightweight alternative to JPA. It focuses on simple, predictable mapping without a persistence context. The latest release is 4.0.2.

## When to Use
- You want a simple SQL mapping model
- You prefer aggregate-based persistence
- You do not need lazy loading or complex ORM features

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jdbc</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-data-jdbc"
```

## Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/app
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
public interface CustomerRepository extends CrudRepository<Customer, Long> {
    List<Customer> findByLastName(String lastName);
}
```

## Notes
- Entities are aggregate roots by design.
- No lazy loading or automatic dirty checking.

## Testing
- Use `@DataJdbcTest` for repository-focused tests.

## References
- [Spring Data JDBC project page](https://spring.io/projects/spring-data-jdbc)
- [Spring Data JDBC reference](https://docs.spring.io/spring-data/jdbc/reference/)
