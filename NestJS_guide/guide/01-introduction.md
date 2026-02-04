# Introduction to NestJS 11 (2026 Edition)

NestJS is a progressive Node.js framework for building efficient, reliable, and scalable server-side applications. It uses TypeScript by default and combines elements of OOP (Object Oriented Programming), FP (Functional Programming), and FRP (Functional Reactive Programming).

**🚀 What's New in NestJS 11**: Native Express v5 support, enhanced performance, TypeScript 5.x compatibility, and improved developer experience.

## Why NestJS 11 in 2026?

| Feature | NestJS 11 | Benefits for Modern Development |
|---------|------------|------------------------------|
| **Express v5 Native** | Built-in Express v5 | Better performance, modern routing |
| **TypeScript 5.x** | Latest TypeScript | Enhanced type safety, performance |
| **Performance Boost** | Optimized DI system | Faster startup, lower memory |
| **Enhanced CLI** | Better DX tools | AI-assisted code generation |
| **Microservices Ready** | Built-in support | Native distributed development |
| **Real-time Features** | WebSockets, Event-Driven | Live collaboration, streaming |
| **Enterprise Patterns** | CQRS, Event Sourcing | Advanced architecture support |

## Installation

## Installation

### Prerequisites (2026 Requirements)

```bash
# Check Node.js version (>= 22.0.0 recommended)
node --version
# v22.x.x or higher recommended for best performance

# Check npm/yarn/pnpm version
npm --version
# OR yarn --version
# OR pnpm --version

# Check TypeScript version (optional, will be installed)
npx tsc --version
# 5.x.x recommended (NestJS 11 compatible)
```

### What's New in NestJS 11

| Feature | Description |
|---------|-------------|
| **Express v5 Integration** | Native Express v5 support with breaking changes |
| **Performance Optimizations** | 15-20% faster startup time |
| **Enhanced DI Container** | Better memory management |
| **Improved CLI Commands** | AI-powered code generation |
| **Modern Testing** | Updated to Jest 29+ |
| **Better Error Handling** | More descriptive error messages |

### Install NestJS CLI

```bash
# Install globally
npm install -g @nestjs/cli

# Verify installation
nest --version
```

### Create New Project

```bash
# Create project with npm
nest new my-project

# Or with specific package manager
nest new my-project --package-manager yarn
nest new my-project --package-manager pnpm
```

## Project Structure

After creation, your project will have this structure:

```
my-project/
├── src/
│   ├── app.controller.spec.ts   # Unit tests for controller
│   ├── app.controller.ts        # Basic controller with a single route
│   ├── app.module.ts            # Root module of the application
│   ├── app.service.ts           # Basic service with a single method
│   └── main.ts                  # Entry file - creates NestJS app instance
├── test/
│   ├── app.e2e-spec.ts          # E2E tests
│   └── jest-e2e.json            # E2E test configuration
├── node_modules/
├── .eslintrc.js                 # ESLint configuration
├── .prettierrc                  # Prettier configuration
├── nest-cli.json                # NestJS CLI configuration
├── package.json                 # Dependencies and scripts
├── tsconfig.json                # TypeScript configuration
└── tsconfig.build.json          # TypeScript build configuration
```

## Core Files Explained

### main.ts - Application Entry Point

```typescript
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  // Create NestJS application instance
  const app = await NestFactory.create(AppModule);

  // Start listening on port 3000
  await app.listen(3000);
}
bootstrap();
```

### app.module.ts - Root Module

```typescript
import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';

@Module({
  imports: [],           // Other modules this module depends on
  controllers: [AppController],  // Controllers belonging to this module
  providers: [AppService],       // Services/providers for this module
})
export class AppModule {}
```

### app.controller.ts - Basic Controller

```typescript
import { Controller, Get } from '@nestjs/common';
import { AppService } from './app.service';

@Controller()  // Base route: /
export class AppController {
  // Dependency injection through constructor
  constructor(private readonly appService: AppService) {}

  @Get()  // GET /
  getHello(): string {
    return this.appService.getHello();
  }
}
```

### app.service.ts - Basic Service

```typescript
import { Injectable } from '@nestjs/common';

@Injectable()  // Marks class as injectable provider
export class AppService {
  getHello(): string {
    return 'Hello World!';
  }
}
```

## Running the Application

```bash
# Development mode (with hot reload)
npm run start:dev

# Production mode
npm run start:prod

# Debug mode
npm run start:debug
```

## Available Scripts

| Command | Description |
|---------|-------------|
| `npm run start` | Start application |
| `npm run start:dev` | Start with watch mode (hot reload) |
| `npm run start:debug` | Start with debug mode |
| `npm run start:prod` | Start production build |
| `npm run build` | Build the application |
| `npm run test` | Run unit tests |
| `npm run test:e2e` | Run end-to-end tests |
| `npm run test:cov` | Run tests with coverage |
| `npm run lint` | Lint code with ESLint |
| `npm run format` | Format code with Prettier |

## NestJS CLI Commands

```bash
# Generate a new module
nest generate module users
# or shorthand
nest g mo users

# Generate a new controller
nest generate controller users
# or shorthand
nest g co users

# Generate a new service
nest generate service users
# or shorthand
nest g s users

# Generate complete resource (module + controller + service + DTOs)
nest generate resource users
# or shorthand
nest g res users
```

### Resource Generator Options

When using `nest g res`, you'll be asked:

```
? What transport layer do you use?
  > REST API
    GraphQL (code first)
    GraphQL (schema first)
    Microservice (non-HTTP)
    WebSockets

? Would you like to generate CRUD entry points? (Y/n)
```

## First API Endpoint

Let's create a simple users endpoint:

```bash
# Generate users resource
nest g res users
```

This creates:

```
src/users/
├── dto/
│   ├── create-user.dto.ts
│   └── update-user.dto.ts
├── entities/
│   └── user.entity.ts
├── users.controller.spec.ts
├── users.controller.ts
├── users.module.ts
└── users.service.ts
```

### Generated Controller

```typescript
// src/users/users.controller.ts
import {
  Controller, Get, Post, Body, Patch, Param, Delete
} from '@nestjs/common';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';

@Controller('users')  // Route prefix: /users
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Post()  // POST /users
  create(@Body() createUserDto: CreateUserDto) {
    return this.usersService.create(createUserDto);
  }

  @Get()  // GET /users
  findAll() {
    return this.usersService.findAll();
  }

  @Get(':id')  // GET /users/:id
  findOne(@Param('id') id: string) {
    return this.usersService.findOne(+id);
  }

  @Patch(':id')  // PATCH /users/:id
  update(@Param('id') id: string, @Body() updateUserDto: UpdateUserDto) {
    return this.usersService.update(+id, updateUserDto);
  }

  @Delete(':id')  // DELETE /users/:id
  remove(@Param('id') id: string) {
    return this.usersService.remove(+id);
  }
}
```

## Request Lifecycle

Understanding how NestJS processes a request:

```
                         ┌─────────────────────────────────────────────────────────────┐
                         │                    NestJS Request Lifecycle                  │
                         └─────────────────────────────────────────────────────────────┘

Incoming Request ──────►  Middleware ──────►  Guards ──────►  Interceptors (before)
                                                                      │
                                                                      ▼
                                                              Pipes (validation)
                                                                      │
                                                                      ▼
                                                               Route Handler
                                                                      │
                                                                      ▼
                                                            Interceptors (after)
                                                                      │
                                                                      ▼
                                                            Exception Filters
                                                                      │
                                                                      ▼
                                                              Server Response
```

## Configuration with Environment Variables

### Install ConfigModule

```bash
npm install @nestjs/config
```

### Setup Configuration

```typescript
// src/app.module.ts
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,  // Available in all modules
      envFilePath: '.env',
    }),
  ],
})
export class AppModule {}
```

### Create .env File

```env
# .env
PORT=3000
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_USER=postgres
DATABASE_PASSWORD=secret
DATABASE_NAME=mydb
JWT_SECRET=your-secret-key
```

### Use Configuration

```typescript
// src/main.ts
import { ConfigService } from '@nestjs/config';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  const configService = app.get(ConfigService);

  const port = configService.get<number>('PORT') || 3000;
  await app.listen(port);

  console.log(`Application running on port ${port}`);
}
```

## Best Practices

| Practice | Description |
|----------|-------------|
| Use CLI generators | Consistent file structure and boilerplate |
| One module per feature | Keep related code together |
| Interface for DTOs | Define clear contracts |
| Environment variables | Never hardcode secrets |
| Validation | Always validate input data |
| Error handling | Use built-in exception filters |

---

## Next Steps

- [Modules and Controllers](./02-modules-controllers.md) - Deep dive into modular architecture

---

[Back to Index](./README.md) | [Next: Modules and Controllers →](./02-modules-controllers.md)
