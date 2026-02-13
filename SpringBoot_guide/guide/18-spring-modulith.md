# 18 - Spring Modulith

Spring Modulith helps you build modular monoliths by enforcing module boundaries, providing verification, observability, and documentation support. The latest stable version is 2.0.2.

## When to Use
- You want a modular monolith with clear boundaries
- You need architectural verification and module-level tests
- You plan to evolve toward distributed services later

## Dependency Management
Use the Spring Modulith BOM to keep module versions aligned.

### Maven
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.modulith</groupId>
      <artifactId>spring-modulith-bom</artifactId>
      <version>2.0.2</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### Gradle
```gradle
implementation platform("org.springframework.modulith:spring-modulith-bom:2.0.2")
```

## Starters
Choose starters based on your persistence stack:
- `spring-modulith-starter-core`
- `spring-modulith-starter-jpa`
- `spring-modulith-starter-jdbc`
- `spring-modulith-starter-mongodb`
- `spring-modulith-starter-neo4j`
- `spring-modulith-starter-insight`
- `spring-modulith-starter-test`

## Defining Modules
Organize modules as direct sub-packages of your application base package. Optional `@ApplicationModule` metadata can define allowed dependencies.

```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "shared" }
)
package com.example.shop.orders;
```

## Verification and Documentation
Spring Modulith can verify module boundaries, generate documentation snippets, and provide runtime insights through observability components.

## References
- [Spring Modulith project page](https://spring.io/projects/spring-modulith)
- [Spring Modulith reference](https://docs.spring.io/spring-modulith/reference/)
