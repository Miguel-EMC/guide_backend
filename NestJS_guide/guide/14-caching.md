# Caching

Caching reduces response latency and relieves pressure on databases and downstream APIs. NestJS integrates with `cache-manager` via `@nestjs/cache-manager`.

## Goals

- Configure global caching
- Cache specific responses
- Use a distributed cache in production

## Install

```bash
npm install @nestjs/cache-manager cache-manager
```

## Global Cache Module

```typescript
// src/app.module.ts
import { Module } from '@nestjs/common';
import { CacheModule } from '@nestjs/cache-manager';

@Module({
  imports: [
    CacheModule.register({
      ttl: 10_000,
      max: 100,
      isGlobal: true,
    }),
  ],
})
export class AppModule {}
```

`ttl` units can vary by store and version. Confirm the unit in your cache-manager and store documentation.

## Manual Cache Usage

```typescript
import { CACHE_MANAGER, Inject, Injectable } from '@nestjs/common';
import type { Cache } from 'cache-manager';

@Injectable()
export class PostsService {
  constructor(@Inject(CACHE_MANAGER) private readonly cache: Cache) {}

  async findOne(id: number) {
    const key = `post:${id}`;
    const cached = await this.cache.get(key);
    if (cached) return cached;

    const post = { id, title: 'Hello' };
    await this.cache.set(key, post, 60_000);
    return post;
  }
}
```

## Cache Responses Automatically

```typescript
import { CacheInterceptor } from '@nestjs/cache-manager';
import { UseInterceptors, Controller, Get } from '@nestjs/common';

@Controller('posts')
@UseInterceptors(CacheInterceptor)
export class PostsController {
  @Get()
  list() {
    return [{ id: 1, title: 'Cached' }];
  }
}
```

You can override TTL per handler:

```typescript
import { CacheTTL } from '@nestjs/cache-manager';

@CacheTTL(5_000)
@Get('hot')
hot() {
  return { hot: true };
}
```

## Distributed Cache (Redis)

For multi-instance deployments, use a shared cache like Redis. Use a cache-manager store that supports your Redis client and configure it in `CacheModule.register` or `registerAsync`.

## Cache Invalidation

Cache invalidation is as important as caching. Always clear or update cache entries when you mutate the underlying data.

```typescript
await this.cache.del(`post:${id}`);
```

## Tips

- Cache only data that is expensive to compute.
- Keep TTLs short for frequently changing data.
- Prefer explicit cache keys with clear prefixes.

---

[Previous: Project Blog API](./13-project-blog-api.md) | [Back to Index](./README.md) | [Next: Rate Limiting ->](./15-rate-limiting.md)
