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
    service/
    repository/
src/main/resources
  application.yml
  static/
  templates/
```

## What You Will Build
This guide walks from fundamentals to advanced production topics:
- REST APIs and security
- Data persistence and caching
- Messaging and integration
- Testing, observability, and deployment

## References
- [Spring Boot project page](https://spring.io/projects/spring-boot)
- [Spring Boot reference](https://docs.spring.io/spring-boot/reference/)
