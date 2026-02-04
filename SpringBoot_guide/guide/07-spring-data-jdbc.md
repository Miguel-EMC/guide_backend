# 07 - Spring Data JDBC

Spring Data JDBC is a sub-project of Spring Data that provides support for JDBC-based repositories. It is a lightweight alternative to Spring Data JPA and is a good choice for applications that do not require the full power of JPA.

To use Spring Data JDBC, you will need to add the `spring-boot-starter-data-jdbc` dependency to your project.

Here is an example of a Spring Data JDBC repository:

```java
@Repository
public interface MyRepository extends CrudRepository<MyEntity, Long> {

    List<MyEntity> findByStatus(String status);
}
```

As you can see, the code is very similar to the Spring Data JPA example. The main difference is that we have extended the `CrudRepository` interface instead of the `JpaRepository` interface.
