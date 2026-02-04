# 14 - Spring for GraphQL

Spring for GraphQL is a framework that provides support for GraphQL. It is built on top of GraphQL Java and provides a secure and extensible way to build your own GraphQL APIs.

To use Spring for GraphQL, you will need to add the `spring-boot-starter-graphql` dependency to your project.

Once you have added the dependency, you can create a GraphQL controller by using the `@Controller` annotation and the `@QueryMapping`, `@MutationMapping`, and `@SubscriptionMapping` annotations.

Here is an example of a simple GraphQL controller:

```java
@Controller
public class MyController {

    @QueryMapping
    public MyEntity myEntity(@Argument Long id) {
        // ...
    }

    @MutationMapping
    public MyEntity createMyEntity(@Argument MyEntityInput input) {
        // ...
    }
}
```
