# 08 - Spring Data MongoDB

Spring Data MongoDB is a sub-project of Spring Data that provides support for MongoDB. It provides a familiar and consistent, Spring-based programming model for interacting with MongoDB.

To use Spring Data MongoDB, you will need to add the `spring-boot-starter-data-mongodb` dependency to your project.

Here is an example of a Spring Data MongoDB repository:

```java
@Repository
public interface MyRepository extends MongoRepository<MyEntity, String> {

    List<MyEntity> findByStatus(String status);
}
```

In this example, we have created a repository for the `MyEntity` entity. We have extended the `MongoRepository` interface, which provides us with a number of methods for interacting with the database, such as `save()`, `findAll()`, and `findById()`.

We have also defined a custom method, `findByStatus()`, which will allow us to find all of the entities that have a certain status.
