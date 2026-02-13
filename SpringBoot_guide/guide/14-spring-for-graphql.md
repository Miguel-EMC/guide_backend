# 14 - Spring for GraphQL

Spring for GraphQL provides GraphQL server support on top of GraphQL Java. The latest release is 2.0.2.

## When to Use
- You need flexible query shapes for clients
- You want a single endpoint with typed schema and resolvers
- You need subscriptions for real-time updates

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-graphql"
```

## Schema (resources/graphql/schema.graphqls)
```graphql
type Query {
  bookById(id: ID!): Book
}

type Book {
  id: ID!
  title: String!
  author: String!
}
```

## Controller Example
```java
@Controller
public class BookController {

    @QueryMapping
    public Book bookById(@Argument String id) {
        return new Book(id, "Spring in Action", "Walls");
    }
}
```

## Testing
- Use `@GraphQlTest` for slice tests.

## References
- [Spring for GraphQL project page](https://spring.io/projects/spring-graphql)
- [Spring for GraphQL reference](https://docs.spring.io/spring-graphql/reference/)
