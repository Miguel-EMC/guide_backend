# 10 - Spring Data R2DBC

Spring Data R2DBC is a sub-project of Spring Data that provides support for R2DBC. R2DBC stands for Reactive Relational Database Connectivity and is a specification for reactive drivers for SQL databases.

To use Spring Data R2DBC, you will need to add the `spring-boot-starter-data-r2dbc` dependency to your project.

Here is an example of a Spring Data R2DBC repository:

```java
@Repository
public interface MyRepository extends ReactiveCrudRepository<MyEntity, Long> {

    Flux<MyEntity> findByStatus(String status);
}
```

In this example, we have created a repository for the `MyEntity` entity. We have extended the `ReactiveCrudRepository` interface, which provides us with a number of methods for interacting with the database in a reactive way, such as `save()`, `findAll()`, and `findById()`.

We have also defined a custom method, `findByStatus()`, which will allow us to find all of the entities that have a certain status.
