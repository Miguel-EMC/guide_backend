# Deployment

This chapter covers production deployment patterns for NestJS with Docker, environment validation, and graceful shutdown.

## Goals

- Build a production-ready Docker image
- Validate environment variables at startup
- Run migrations and health checks safely

## Environment Configuration

Use `@nestjs/config` to keep environment handling consistent.

```typescript
// src/config/configuration.ts
export default () => ({
  port: parseInt(process.env.PORT ?? '3000', 10),
  databaseUrl: process.env.DATABASE_URL,
  jwtAccessSecret: process.env.JWT_ACCESS_SECRET,
});
```

```typescript
// src/app.module.ts
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import configuration from './config/configuration';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      load: [configuration],
    }),
  ],
})
export class AppModule {}
```

## Environment Validation

Use a schema to fail fast on missing vars.

```typescript
// src/config/env.validation.ts
import * as Joi from 'joi';

export const validationSchema = Joi.object({
  NODE_ENV: Joi.string().valid('development', 'test', 'production').required(),
  PORT: Joi.number().default(3000),
  DATABASE_URL: Joi.string().required(),
  JWT_ACCESS_SECRET: Joi.string().required(),
});
```

```typescript
ConfigModule.forRoot({
  isGlobal: true,
  validationSchema,
  validationOptions: { abortEarly: true },
});
```

## Graceful Shutdown

```typescript
// src/main.ts
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableShutdownHooks();
  await app.listen(process.env.PORT ?? 3000);
}
bootstrap();
```

## Dockerfile (Multi-stage)

```dockerfile
# syntax=docker/dockerfile:1
FROM node:24-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci

FROM node:24-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:24-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm ci --omit=dev && npm cache clean --force
COPY --from=builder /app/dist ./dist

USER node
EXPOSE 3000
CMD ["node", "dist/main.js"]
```

Use the Active LTS image for production (24.x at the time of writing). You can also use a Maintenance LTS line (22.x or 20.x), but avoid versions below Node 20 because NestJS 11 requires Node >= 20.

## Running Migrations

Run migrations before starting the app. Do this in CI/CD or entrypoint scripts.

```bash
npm run migration:run
```

## Health Check

Expose a simple health endpoint.

```typescript
import { Controller, Get } from '@nestjs/common';

@Controller('health')
export class HealthController {
  @Get()
  check() {
    return { status: 'ok', ts: new Date().toISOString() };
  }
}
```

## Production Tips

- Use HTTPS behind a reverse proxy.
- Set `NODE_ENV=production` in runtime.
- Use separate configs for dev, test, and prod.

---

[Previous: Testing](./11-testing.md) | [Back to Index](./README.md) | [Next: Project Blog API ->](./13-project-blog-api.md)
