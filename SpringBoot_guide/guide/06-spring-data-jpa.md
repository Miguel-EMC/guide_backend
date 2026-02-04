# 06 - Spring Data JPA

Spring Data JPA is a sub-project of Spring Data that makes it easy to implement JPA-based repositories. It provides a set of interfaces and classes that allow you to easily interact with your database.

To use Spring Data JPA, you will need to add the `spring-boot-starter-data-jpa` dependency to your project.

Here is an example of a Spring Data JPA repository:

```java
@Repository
public interface MyRepository extends JpaRepository<MyEntity, Long> {

    List<MyEntity> findByStatus(String status);
}
```

In this example, we have created a repository for the `MyEntity` entity. We have extended the `JpaRepository` interface, which provides us with a number of methods for interacting with the database, such as `save()`, `findAll()`, and `findById()`.

We have also defined a custom method, `findByStatus()`, which will allow us to find all of the entities that have a certain status.
