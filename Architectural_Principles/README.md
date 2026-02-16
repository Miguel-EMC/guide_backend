# Architectural Principles

This section contains high-level guides on software design principles and architectural patterns that are applicable across different languages and frameworks.

---

## Chapters

### Fundamentals
1. [SOLID Principles](./01-SOLID-Principles.md) - The five principles of object-oriented design
2. [Design Patterns Overview](./02-Design-Patterns-Overview.md) - Singleton, Factory, Adapter, Decorator, Observer, Strategy

### Architecture
3. [Hexagonal Architecture](./03-Hexagonal-Architecture.md) - Ports & Adapters, isolating business logic
4. [Clean Architecture](./04-Clean-Architecture.md) - Layers, dependency rule, use cases
5. [Domain-Driven Design](./05-Domain-Driven-Design.md) - Entities, Value Objects, Aggregates, Bounded Contexts

### Patterns
6. [Repository Pattern](./06-Repository-Pattern.md) - Data access abstraction, Unit of Work
7. [Advanced Design Patterns](./07-Advanced-Design-Patterns.md) - Builder, Facade, Proxy, Command, Chain of Responsibility, State, Template Method
8. [Microservices Patterns](./08-Microservices-Patterns.md) - Circuit Breaker, Saga, Event Sourcing, CQRS, Service Discovery

---

## Quick Reference

| Topic | Key Concepts |
|-------|--------------|
| **SOLID** | SRP, OCP, LSP, ISP, DIP |
| **Hexagonal** | Ports, Adapters, Domain isolation |
| **Clean Architecture** | Entities, Use Cases, Adapters, Frameworks |
| **DDD** | Ubiquitous Language, Bounded Contexts, Aggregates |
| **Repository** | Collection abstraction, Unit of Work |
| **Microservices** | Circuit Breaker, Saga, CQRS, Event Sourcing |

---

## When to Use What

| Scenario | Recommended Patterns |
|----------|---------------------|
| Simple CRUD app | Repository Pattern |
| Complex business logic | DDD + Hexagonal/Clean Architecture |
| Multiple interfaces (API, CLI, etc.) | Hexagonal Architecture |
| High read/write asymmetry | CQRS |
| Distributed transactions | Saga Pattern |
| Audit requirements | Event Sourcing |
| External service calls | Circuit Breaker, Retry, Bulkhead |

---

_Guide created by Miguel Muzo ([@migueldev11](https://github.com/migueldev11))_
