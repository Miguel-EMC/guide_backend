# 34 - Microservices Architecture

This chapter provides a practical blueprint for building microservices with Spring Boot. It focuses on real-world concerns: boundaries, communication, resilience, data consistency, observability, security, and deployment.

## When to Choose Microservices
- Teams need independent release cycles and ownership.
- The system is large enough that modular monoliths struggle.
- You require isolated scaling or fault containment.

If you can keep a modular monolith, do so first. Microservices add operational cost.

## Core Principles
- **Single responsibility per service** (bounded contexts).
- **Database per service** to avoid tight coupling.
- **API-first contracts** for versioning and compatibility.
- **Automate everything**: builds, tests, and deployments.

## Typical Microservices Topology
```
Clients
  |
API Gateway
  |
  +--> Auth Service -----> User DB
  +--> Orders Service ---> Orders DB
  |        |
  |        +--> Inventory Service ---> Inventory DB
  +--> Billing Service ---> Payments DB
  +--> Notification Service ---> Broker/Email/SMS
```

## Service Boundaries
- Organize by domain, not technical layers.
- Avoid shared databases.
- Share only contracts and schemas, not internal models.

## Communication Patterns
### Synchronous
- REST or gRPC for request/response.
- Use timeouts, retries, and circuit breakers.
- Keep synchronous chains short to reduce latency.

### Asynchronous
- Publish events to a broker for decoupling.
- Prefer events as facts, not commands.
- Use idempotency keys for handlers.

## Resilience Patterns
- Circuit breakers to stop cascading failures.
- Bulkheads to isolate resource usage.
- Rate limits at the gateway.
- Retries with exponential backoff.

## Data Consistency
- **Saga** for multi-service workflows.
- **Outbox** for reliable event publishing.
- **CDC** for integration with legacy systems.
- Avoid distributed transactions when possible.

## API Gateway
Responsibilities:
- Routing and aggregation
- Authentication and authorization
- Rate limiting and throttling
- Observability headers (trace IDs)

## Configuration and Secrets
- Central config server or Vault-based config.
- Use environment variables for secrets.
- Externalize all environment-specific settings.

## Observability
- Structured logs with correlation IDs.
- Metrics (SLIs/SLOs) for latency, error rate, saturation.
- Tracing for cross-service requests.

## Security
- OAuth2/OIDC for user authentication.
- mTLS or JWT for service-to-service calls.
- Fine-grained authorization at each service.

## Deployment
- Containerize each service.
- Use Kubernetes or managed platforms.
- Separate dev/staging/prod environments.
- Canary or blue/green deployments for safety.

## Testing Strategy
- Unit tests for business logic.
- Contract tests for APIs.
- Integration tests for brokers and databases.
- End-to-end tests only for critical flows.

## Practical Checklist
- Clear bounded context per service.
- Service owns its data.
- Tracing + metrics + logs are enabled.
- CI/CD pipeline with automated quality gates.
- Rollback and migration strategies in place.

## Example Project
See the mini project at:
`SpringBoot_guide/examples/microservices/`

## References
- [Spring Boot reference](https://docs.spring.io/spring-boot/reference/)
- [Spring Cloud project page](https://spring.io/projects/spring-cloud)
- [Resilience4j project page](https://resilience4j.readme.io/)
- [OpenTelemetry project page](https://opentelemetry.io/)
