# 05 - Working with Spring Data

Spring Data is an umbrella project that provides a consistent, Spring-based programming model for data access across relational and non-relational stores. The current release train is 2025.1.2.

## When to Use
- You want a consistent repository abstraction across different data stores.
- You need standard CRUD operations plus rich querying and pagination.
- You want built-in auditing support for created/updated metadata.

## Core Concepts
- **Repository abstraction:** Define interfaces that extend `Repository`, `CrudRepository`, or `ListCrudRepository` to get CRUD behavior.
- **Query derivation:** Method names can be parsed into queries (for example, `findByLastName`).
- **Custom queries:** Use store-specific annotations like `@Query` when derivation isn't enough.
- **Auditing:** Add annotations like `@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`, and `@LastModifiedDate` to track entity changes.

## Key Modules
Spring Data provides modules for multiple stores, including JPA, JDBC, R2DBC, MongoDB, Redis, Cassandra, Neo4j, LDAP, and REST.

## Dependency Management
- **With Spring Boot:** Use the appropriate starter (for example, `spring-boot-starter-data-jpa`, `spring-boot-starter-data-mongodb`, or `spring-boot-starter-data-redis`). Boot manages compatible versions for you.
- **Without Spring Boot:** Import the Spring Data release train BOM to align module versions.

## Basic Repository Example
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
public interface CustomerRepository extends CrudRepository<Customer, Long> {
    List<Customer> findByLastName(String lastName);
}
```

## Auditing Example (JPA)
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

## Best Practices
- Prefer `Slice` or `Page` for large datasets.
- Use projections to avoid over-fetching.
- Keep repository interfaces focused on a single aggregate.

## References
- [Spring Data project page](https://spring.io/projects/spring-data)
- [Spring Data auditing reference](https://docs.spring.io/spring-data/jpa/reference/#auditing)
- [Spring Data Commons reference](https://docs.spring.io/spring-data/commons/reference/)
