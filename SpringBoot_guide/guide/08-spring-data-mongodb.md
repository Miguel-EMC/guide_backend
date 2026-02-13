# 08 - Spring Data MongoDB

Spring Data MongoDB provides repository support, template APIs, and reactive access for MongoDB. The latest release is 5.0.2.

## When to Use
- You want a document database with flexible schema
- You need JSON-like storage and aggregation pipelines
- You want both imperative and reactive options

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-data-mongodb"
```

For reactive access:
```gradle
implementation "org.springframework.boot:spring-boot-starter-data-mongodb-reactive"
```

## Configuration
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/app
```

## Repository Example
```java
@Document("customers")
public class Customer {
    @Id
    private String id;
    private String firstName;
    private String lastName;
}
```

```java
public interface CustomerRepository extends MongoRepository<Customer, String> {
    List<Customer> findByLastName(String lastName);
}
```

## Notes
- Prefer `MongoTemplate` for complex aggregations.
- For large collections, use paging and projections.

## References
- [Spring Data MongoDB project page](https://spring.io/projects/spring-data-mongodb)
- [Spring Data MongoDB reference](https://docs.spring.io/spring-data/mongodb/reference/)
