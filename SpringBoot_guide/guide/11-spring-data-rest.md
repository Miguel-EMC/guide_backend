# 11 - Spring Data REST

Spring Data REST exposes Spring Data repositories as REST resources, with support for HAL, pagination, projections, and metadata. The latest stable version is 5.0.2.

## When to Use
- Rapid internal APIs for admin or back-office use
- Prototyping and read-heavy data services
- Teams that are already committed to Spring Data repositories

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-rest</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-data-rest"
```

## Basic Example
```java
@Entity
public class Book {
    @Id
    private Long id;
    private String title;
    private String author;
}
```

```java
@RepositoryRestResource(path = "books")
public interface BookRepository extends PagingAndSortingRepository<Book, Long> {
    List<Book> findByAuthor(String author);
}
```

By default, repository query methods are exposed under `/books/search`.

## Customizing Exposure
- Use `@RepositoryRestResource(path = "...")` to customize the collection resource path.
- Use `@RestResource(exported = false)` to hide a repository method or association.
- Use projections to expose only selected fields.

## Key Features
- HAL and pagination out of the box
- Event hooks for create/update/delete
- ALPS and JSON Schema metadata
- HATEOAS-friendly responses
- HAL Explorer for browser-based exploration

## When Not to Use
- Public APIs that require strict versioning or custom response contracts
- Workflows that need non-CRUD operations as the primary interface

## References
- [Spring Data REST project page](https://spring.io/projects/spring-data-rest)
- [Spring Data REST reference](https://docs.spring.io/spring-data/rest/reference/)
