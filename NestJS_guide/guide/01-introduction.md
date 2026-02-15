# Introduction to NestJS (2026 Edition)

NestJS is a progressive Node.js framework for building efficient, scalable server-side applications. It is built with TypeScript, uses a modular architecture, and provides first-class dependency injection.

## Version Guidance (2026)

| Stack | Recommended | Notes |
| --- | --- | --- |
| NestJS | 11.x | Current major release line. |
| Node.js | 24.x | Active LTS for production. |
| Node.js | 25.x | Current release line for local dev only. |
| Node.js | 22.x / 20.x | Maintenance LTS (still supported). |
| TypeScript | 5.8.x | Current stable release line. |
| Express | 5.x | Current major and the default adapter in NestJS 11. |

NestJS 11 requires Node.js 20 or higher.

## Why NestJS

- Opinionated structure without losing flexibility.
- Built-in DI, testing utilities, and transport adapters.
- Works with Express or Fastify, plus microservices and WebSockets.

## Prerequisites

```bash
node --version
npm --version
```

Use an Active LTS Node.js release (currently 24.x) for production. Use the current line (25.x) only if your dependencies support it.

## Install the Nest CLI

```bash
npm install -g @nestjs/cli
nest --version
```

## Create a New Project

```bash
nest new my-api
```

### With a specific package manager

```bash
nest new my-api --package-manager pnpm
```

## Project Structure

```
my-api/
|-- src/
|   |-- app.controller.ts
|   |-- app.controller.spec.ts
|   |-- app.module.ts
|   |-- app.service.ts
|   `-- main.ts
|-- test/
|   |-- app.e2e-spec.ts
|   `-- jest-e2e.json
|-- nest-cli.json
|-- package.json
|-- tsconfig.json
`-- tsconfig.build.json
```

## Request Lifecycle Overview

```
Incoming Request
  -> Middleware
  -> Guards
  -> Interceptors (before)
  -> Pipes (validation and transform)
  -> Route Handler
  -> Interceptors (after)
  -> Exception Filters
  -> Response
```

## First Controller and Service

```typescript
// src/app.controller.ts
import { Controller, Get } from '@nestjs/common';
import { AppService } from './app.service';

@Controller()
export class AppController {
  constructor(private readonly appService: AppService) {}

  @Get()
  health() {
    return this.appService.health();
  }
}
```

```typescript
// src/app.service.ts
import { Injectable } from '@nestjs/common';

@Injectable()
export class AppService {
  health() {
    return { status: 'ok', ts: new Date().toISOString() };
  }
}
```

## Running the App

```bash
npm run start:dev
```

## Express vs Fastify

NestJS supports multiple HTTP adapters. Express is the default, Fastify is a high-performance alternative.

```bash
npm install @nestjs/platform-fastify
```

```typescript
// src/main.ts
import { NestFactory } from '@nestjs/core';
import { FastifyAdapter, NestFastifyApplication } from '@nestjs/platform-fastify';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create<NestFastifyApplication>(
    AppModule,
    new FastifyAdapter(),
  );
  await app.listen({ port: 3000, host: '0.0.0.0' });
}
bootstrap();
```

## Environment Configuration

```bash
npm install @nestjs/config
```

```typescript
// src/app.module.ts
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),
  ],
})
export class AppModule {}
```

```env
# .env
PORT=3000
NODE_ENV=development
```

## Common CLI Commands

```bash
nest g module users
nest g controller users
nest g service users
nest g resource users
```

## Next Steps

- [Modules and Controllers](./02-modules-controllers.md)

---

[Previous: Index](./README.md) | [Back to Index](./README.md) | [Next: Modules and Controllers ->](./02-modules-controllers.md)
