# 11 - Spring Data REST

Spring Data REST is a sub-project of Spring Data that allows you to expose your Spring Data repositories as REST APIs. It is a very powerful tool that can save you a lot of time and effort when building REST APIs.

To use Spring Data REST, you will need to add the `spring-boot-starter-data-rest` dependency to your project.

Once you have added the dependency, Spring Data REST will automatically expose your repositories as REST APIs. For example, if you have a `MyRepository` interface, Spring Data REST will expose a `/myEntities` endpoint that you can use to interact with your `MyEntity` entities.
