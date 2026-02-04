# 09 - Spring Data Redis

Spring Data Redis is a sub-project of Spring Data that provides support for Redis. It provides a familiar and consistent, Spring-based programming model for interacting with Redis.

To use Spring Data Redis, you will need to add the `spring-boot-starter-data-redis` dependency to your project.

Here is an example of a Spring Data Redis repository:

```java
@Repository
public interface MyRepository extends CrudRepository<MyEntity, String> {

    List<MyEntity> findByStatus(String status);
}
```
