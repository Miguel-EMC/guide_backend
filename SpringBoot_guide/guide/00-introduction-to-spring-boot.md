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

## Typical Project Structure (Layered)
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

## Feature-Based Structure (By Domain)
```
src/main/java
  com.example.app
    Application.java
    shared/
      config/
      security/
      util/
    orders/
      web/
        OrderController.java
        dto/
      service/
        OrderService.java
      repository/
        OrderRepository.java
      domain/
        Order.java
        OrderStatus.java
    customers/
      web/
      service/
      repository/
      domain/
```

## Hexagonal (Ports and Adapters)
```
src/main/java
  com.example.app
    Application.java
    config/
    domain/
      model/
      service/
      port/
        in/
        out/
    adapter/
      in/
        web/
      out/
        persistence/
        messaging/
        http/
```

## Modular Monolith (Spring Modulith Style)
```
src/main/java
  com.example.app
    Application.java
    shared/
    orders/
      package-info.java
      api/
      internal/
    inventory/
      package-info.java
      api/
      internal/
```

## Multi-Module (Maven/Gradle)
```
root/
  build.gradle.kts (or pom.xml)
  settings.gradle.kts
  app/
    src/main/java
  common/
    src/main/java
  persistence/
    src/main/java
  integration/
    src/main/java
```

## Microservices (Monorepo Example)
```
root/
  services/
    orders-service/
      src/main/java
      src/main/resources
    billing-service/
      src/main/java
      src/main/resources
    inventory-service/
      src/main/java
      src/main/resources
  libs/
    shared-kernel/
    observability/
  infrastructure/
    docker/
    kubernetes/
    terraform/
  gateway/
    src/main/java
```

## Microservices (Multi-Repo Example)
```
orders-service/
billing-service/
inventory-service/
api-gateway/
shared-kernel/
infra/
```

## Choosing a Style
- Use layered for simple CRUD apps.
- Use feature-based to scale teams by domain.
- Use hexagonal for complex integrations and testing.
- Use modular monolith to enforce boundaries without network cost.
- Use microservices when teams and deployments must be independent.

## Microservices Architecture (Real-World Overview)
```
Client
  |
API Gateway
  |
  +--> Auth Service -----> User DB
  |
  +--> Orders Service ---> Orders DB
  |        |
  |        +--> Inventory Service ---> Inventory DB
  |
  +--> Billing Service ---> Payments DB
  |
  +--> Notification Service ---> Messaging Broker
```

## Synchronous Communication
- REST for broad client compatibility and easy debugging.
- gRPC for high-performance internal calls and streaming.
- Apply timeouts, retries, and circuit breakers for resilience.

## Asynchronous Communication
- Event-driven flows for decoupling and scale.
- Use message brokers for reliable delivery and backpressure.
- Design events as immutable facts, not commands.

## Configuration and Discovery
- Centralized config for secrets and shared settings.
- Service discovery for dynamic scaling.
- API gateway for routing, auth, and rate limiting.

## Security
- OAuth2/OpenID Connect for end-user auth.
- mTLS or signed JWT for service-to-service trust.
- Principle of least privilege for every service.

## Observability
- Metrics, logs, and traces as first-class concerns.
- Correlation IDs across services for traceability.
- Dashboards and alerts tied to SLOs.

## Data Strategy
- Database per service to reduce coupling.
- Use sagas or outbox patterns for distributed consistency.
- Avoid synchronous cross-service transactions.

## Deployment
- Containerize each service.
- Use Kubernetes or managed platforms for scaling.
- Automate builds and releases with CI/CD.

## What You Will Build
This guide walks from fundamentals to advanced production topics:
- REST APIs and security
- Data persistence and caching
- Messaging and integration
- Testing, observability, and deployment

## References
- [Spring Boot project page](https://spring.io/projects/spring-boot)
- [Spring Boot reference](https://docs.spring.io/spring-boot/reference/)
