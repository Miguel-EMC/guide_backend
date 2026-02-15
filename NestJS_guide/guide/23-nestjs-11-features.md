# NestJS 11 Features and Migration Notes

This chapter highlights the main changes in NestJS 11 and how to migrate safely.

## Goals

- Understand the major platform changes
- Update dependencies safely
- Avoid common migration pitfalls

## Express v5 Is the Default

NestJS 11 uses Express v5 by default. Express v5 introduces breaking changes, including updated path matching rules. If you use wildcards, update them to a named wildcard.

```typescript
// Before (Express v4 style)
@Get('users/*')
findAll() {}

// After (Express v5 style)
@Get('users/*splat')
findAll() {}
```

## Query Parser Change (Express v5)

Express v5 now uses the `simple` query parser by default instead of `extended` (which uses `qs`). If you rely on nested query objects, switch the query parser back to `extended`.

```typescript
import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);
  app.set('query parser', 'extended');
  await app.listen(3000);
}
bootstrap();
```

## Fastify v5 Support

`@nestjs/platform-fastify` v11 supports Fastify v5. If you run Fastify, review the Fastify v5 migration guide and enable any CORS methods you need.

```typescript
const methods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];
app.enableCors({ methods });
```

## Node.js Requirement

NestJS 11 requires Node.js v20 or higher. Node 16 and 18 are no longer supported. Use an LTS release in production.

## Dynamic Module Deduplication Change

NestJS 11 changed how dynamic module instances are deduplicated. If you rely on a shared dynamic module across multiple imports, assign it to a variable and reuse it. In tests, you can opt back into the previous algorithm with:

```typescript
Test.createTestingModule({}, { moduleIdGeneratorAlgorithm: 'deep-hash' });
```

## Upgrade Checklist

1. Update packages and lockfile.
2. Run unit and E2E tests.
3. Verify route matching for wildcard paths.
4. Confirm your Node.js version is >= 20.
5. Review Fastify or Express migration docs if applicable.

---

[Previous: Common Patterns](./22-common-patterns.md) | [Back to Index](./README.md) | [Next: Infrastructure as Code ->](./24-infrastructure-iac.md)
