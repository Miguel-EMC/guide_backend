# API Versioning

API versioning lets you ship breaking changes while keeping older clients working. NestJS supports multiple versioning strategies.

## Goals

- Enable versioning globally
- Use per-controller and per-route versions
- Choose the right versioning strategy

## Enable Versioning

```typescript
// src/main.ts
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { VersioningType } from '@nestjs/common';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableVersioning({
    type: VersioningType.URI,
    defaultVersion: '1',
  });
  await app.listen(3000);
}
bootstrap();
```

This will enable URLs like `/v1/posts`.

## Versioned Controllers

```typescript
import { Controller, Get, Version } from '@nestjs/common';

@Controller('posts')
export class PostsController {
  @Version('1')
  @Get()
  listV1() {
    return { version: 1 };
  }

  @Version('2')
  @Get()
  listV2() {
    return { version: 2 };
  }
}
```

## Alternative Strategies

### Header Versioning

```typescript
app.enableVersioning({
  type: VersioningType.HEADER,
  header: 'x-api-version',
});
```

### Media Type Versioning

```typescript
app.enableVersioning({
  type: VersioningType.MEDIA_TYPE,
  key: 'v=',
});
```

## Tips

- Use URI versioning for public APIs.
- Deprecate old versions with clear timelines.
- Keep shared behavior in services to avoid duplication.

---

[Previous: Logging and Monitoring](./20-logging-monitoring.md) | [Back to Index](./README.md) | [Next: Common Patterns ->](./22-common-patterns.md)
