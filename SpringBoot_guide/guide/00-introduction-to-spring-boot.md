# 00 - Introduction to Spring Boot

Spring Boot is an opinionated framework for building stand-alone, production-grade Spring applications. It is built on top of the Spring Framework and focuses on convention, auto-configuration, and a great developer experience. The current line is Spring Boot 4.0.x, which runs on Spring Framework 7 and Java 17 or later.

## Why Spring Boot?
- **Auto-configuration:** Boot configures beans based on classpath and properties.
- **Starters:** Curated dependency sets for common use cases.
- **Embedded servers:** Run without external servlet containers.
- **Production readiness:** Actuator endpoints, health checks, metrics, and tracing.

## Core Concepts
- **ApplicationContext:** The IoC container that manages beans.
- **Externalized configuration:** `application.yml`/`application.properties` with profiles.
- **Profiles:** Environment-specific configuration using `spring.profiles.active`.

## Typical Project Structure
```
src/main/java
  com.example.app
    Application.java
    config/
    web/
      controller/
      dto/
      mapper/
    service/
    repository/
    domain/
      model/
      event/
    integration/
    security/
    jobs/
src/main/resources
  application.yml
  application-dev.yml
  application-prod.yml
  db/migration/
  static/
  templates/
src/test/java
  com.example.app
    web/
    service/
    repository/
    integration/
src/test/resources
  application-test.yml
  testdata/
```

## Notes on Structure
- Keep web, service, and repository layered for clarity in small apps.
- Use `domain/` for core business objects and events.
- Put external adapters in `integration/` to isolate HTTP/DB/broker clients.
- Use `config/` for Spring `@Configuration` and bootstrapping.
- `db/migration/` is commonly used for Flyway or Liquibase migrations.

## What You Will Build
This guide walks from fundamentals to advanced production topics:
- REST APIs and security
- Data persistence and caching
- Messaging and integration
- Testing, observability, and deployment

## References
- [Spring Boot project page](https://spring.io/projects/spring-boot)
- [Spring Boot reference](https://docs.spring.io/spring-boot/reference/)
