# 01 - Introduction to Node.js and Express (2026 Edition)

This guide teaches modern API development with Node.js, Express 5, and TypeScript. You will go from fundamentals to production and scale with real-world patterns.

## Version Guidance (2026)

| Stack | Recommended | Notes |
| --- | --- | --- |
| Node.js | 24.x | Active LTS (production default). |
| Node.js | 25.x | Current (local dev only). |
| Node.js | 22.x / 20.x | Maintenance LTS (still supported). |
| Express | 5.x | Current stable major. |
| TypeScript | 5.x | Latest stable release line. |

Express 5 requires Node.js >= 18. Use Node 24.x LTS for production.

## What Is Node.js

Node.js is a JavaScript runtime built on the V8 engine. It uses an event-driven, non-blocking model that makes it efficient for I/O-heavy workloads like APIs, real-time services, and edge functions.

Key traits:

- Single-threaded event loop with async I/O.
- Huge ecosystem through npm.
- Great fit for APIs, tooling, and streaming workloads.

## What Is Express

Express is a minimal web framework on top of Node.js. It provides routing, middleware, and helpers for building APIs without forcing a heavy structure.

Express gives you:

- A clean routing model.
- Middleware pipelines for auth, validation, and logging.
- Simple request and response helpers.

## Express vs Full-Stack Frameworks

Use Express when you want:

- Full control over architecture and dependencies.
- Minimal abstraction overhead.
- A framework-agnostic way to build APIs.

Use a higher-level framework when you want built-in modules for queues, caching, or dependency injection out of the box.

## Why TypeScript

TypeScript is the standard for professional Node.js backends.

- Safer refactors with static types.
- Clear contracts across teams and modules.
- Better tooling and auto-complete.

## What You Will Build

- A REST API with clean architecture
- JWT auth with refresh tokens
- Validation, testing, and deployment
- Production patterns for scale

## Express 5 Changes You Should Know

Express 5 introduces routing and parsing changes. Examples:

- Wildcard routes require a name (for example `/*splat`).
- The query parser defaults to `simple` (no nested objects).
- `express.urlencoded()` defaults to `extended: false`.

You will see these changes in the routing and request chapters.

## Suggested Architecture (High Level)

- Keep controllers thin and move logic into services.
- Keep database access in a dedicated data layer.
- Use middleware for cross-cutting concerns.

## Production Mindset

From day one:

- Validate inputs.
- Log with request IDs.
- Keep secrets out of code.
- Use LTS Node in production.

## Request Lifecycle (Express View)

Understanding the request path helps you design predictable APIs.

```
Request
  -> global middleware
  -> router-level middleware
  -> route handler
  -> response
  -> error handler (if thrown)
```

## Typical API Layers

Most production Express apps follow this separation:

- Routes: define HTTP endpoints and bind middleware.
- Controllers: map HTTP to service calls.
- Services: implement business logic.
- Data layer: database and external APIs.

## Common Pitfalls

- Blocking the event loop with CPU-heavy work.
- Mixing DB queries directly in route handlers.
- Returning inconsistent response shapes across endpoints.

## Checklist

- Node LTS installed
- TypeScript configured
- Basic health endpoint
- Centralized error handler

---

[Previous: Index](./README.md) | [Back to Index](./README.md) | [Next: Setup ->](./02-setup-node-npm-and-typescript.md)
