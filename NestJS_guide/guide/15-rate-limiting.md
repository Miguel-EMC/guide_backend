# Rate Limiting

Rate limiting protects your API from abuse and keeps latency predictable. NestJS provides `@nestjs/throttler` for IP-based throttling and per-route rules.

## Goals

- Configure global limits
- Customize limits per endpoint
- Skip throttling for trusted routes

## Install

```bash
npm install @nestjs/throttler
```

## Global Throttling

```typescript
// src/app.module.ts
import { Module } from '@nestjs/common';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { APP_GUARD } from '@nestjs/core';

@Module({
  imports: [
    ThrottlerModule.forRoot([
      {
        ttl: 60_000,
        limit: 100,
      },
    ]),
  ],
  providers: [
    {
      provide: APP_GUARD,
      useClass: ThrottlerGuard,
    },
  ],
})
export class AppModule {}
```

## Route-Level Overrides

```typescript
import { Controller, Get } from '@nestjs/common';
import { Throttle, SkipThrottle } from '@nestjs/throttler';

@Controller('health')
export class HealthController {
  @SkipThrottle()
  @Get()
  check() {
    return { ok: true };
  }
}

@Controller('auth')
export class AuthController {
  @Throttle({ ttl: 60_000, limit: 10 })
  @Get('login')
  login() {
    return { ok: true };
  }
}
```

## Custom Key Strategy

By default, throttler uses the request IP as the key. You can customize the key to rate limit by user ID or API key.

```typescript
import { ThrottlerGuard } from '@nestjs/throttler';

export class CustomThrottlerGuard extends ThrottlerGuard {
  protected getTracker(req: Record<string, any>): string {
    return req.user?.id?.toString() ?? req.ip;
  }
}
```

## Tips

- Rate limit auth and write endpoints more aggressively.
- Whitelist internal services with `@SkipThrottle()`.
- Use a distributed store in production if you run multiple instances.

---

[Previous: Caching](./14-caching.md) | [Back to Index](./README.md) | [Next: File Uploads ->](./16-file-uploads.md)
