# Security Hardening

Security at scale requires layered defenses, secure defaults, and continuous review. This chapter outlines practical hardening steps for NestJS APIs.

## Goals

- Reduce attack surface
- Protect data and secrets
- Enforce least privilege

## Transport Security

- Use HTTPS everywhere.
- Terminate TLS at a trusted load balancer.
- Enforce HSTS headers in production.

## HTTP Security Headers

For Express:

```typescript
import helmet from 'helmet';

app.use(helmet());
```

For Fastify, use the Fastify helmet plugin and enable it at bootstrap.

## Input Validation

- Validate all inputs with DTOs and pipes.
- Reject unknown fields with `whitelist` and `forbidNonWhitelisted`.

## Authentication and Sessions

- Use short-lived access tokens.
- Rotate refresh tokens and store them hashed.
- Revoke tokens on logout or compromise.

## Authorization

- Use RBAC or permissions for sensitive routes.
- Combine role checks with resource ownership checks.

## Secrets Management

- Store secrets in a dedicated secrets manager.
- Avoid plaintext `.env` files in production.
- Rotate secrets regularly.

## Dependency Security

- Pin versions and review lockfiles.
- Run dependency scans in CI.

## Data Protection

- Encrypt data at rest with managed database encryption.
- Encrypt sensitive fields at the application level when required.

## Tips

- Remove stack traces from production responses.
- Log security-relevant events with request IDs.
- Add WAF and rate limiting for public endpoints.

---

[Previous: SRE and Operations](./25-sre-operations.md) | [Back to Index](./README.md) | [Next: High-Scale Architecture ->](./27-high-scale-architecture.md)
