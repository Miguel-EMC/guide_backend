# 18 - Spring Modulith

Spring Modulith is an experimental Spring project that helps you structure your Spring Boot application in a modular way. It allows you to create a well-structured monolith that can be easily evolved into a microservices-based architecture if needed.

To use Spring Modulith, you will need to add the `spring-modulith-starter-test` dependency to your project.

Once you have added the dependency, you can create a module by creating a package and adding a `package-info.java` file to it.

Here is an example of a `package-info.java` file:

```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "base" }
)
package com.example.myproject.mymodule;
```
