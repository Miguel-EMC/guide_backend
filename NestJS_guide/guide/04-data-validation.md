# Data Validation

NestJS uses pipes to validate and transform incoming data. The most common approach is `ValidationPipe` with `class-validator` and `class-transformer`.

## Goals

- Validate request payloads with DTOs
- Transform payloads to strong types
- Produce consistent validation errors

## Install Validation Packages

```bash
npm install class-validator class-transformer
```

## Enable Global Validation

```typescript
// src/main.ts
import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
      transformOptions: { enableImplicitConversion: true },
    }),
  );
  await app.listen(3000);
}
bootstrap();
```

## DTO Basics

```typescript
// src/users/dto/create-user.dto.ts
import { IsEmail, IsOptional, IsString, MinLength } from 'class-validator';

export class CreateUserDto {
  @IsEmail()
  email: string;

  @IsString()
  @MinLength(8)
  password: string;

  @IsString()
  @IsOptional()
  name?: string;
}
```

```typescript
// src/users/users.controller.ts
import { Body, Controller, Post } from '@nestjs/common';
import { CreateUserDto } from './dto/create-user.dto';

@Controller('users')
export class UsersController {
  @Post()
  create(@Body() dto: CreateUserDto) {
    return { ok: true, dto };
  }
}
```

## Nested Validation

```typescript
import { Type } from 'class-transformer';
import { IsArray, ValidateNested } from 'class-validator';

class AddressDto {
  @IsString()
  street: string;

  @IsString()
  city: string;
}

export class CreateOrderDto {
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => AddressDto)
  addresses: AddressDto[];
}
```

## Query DTO Example

```typescript
// src/users/dto/list-users-query.dto.ts
import { IsInt, IsOptional, IsString, Min } from 'class-validator';
import { Type } from 'class-transformer';

export class ListUsersQueryDto {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page?: number = 1;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  pageSize?: number = 20;

  @IsOptional()
  @IsString()
  q?: string;
}
```

## Built-in Pipes

```typescript
import { Controller, Get, Param, ParseIntPipe, ParseUUIDPipe } from '@nestjs/common';

@Controller('posts')
export class PostsController {
  @Get(':id')
  get(@Param('id', ParseIntPipe) id: number) {
    return { id };
  }

  @Get('uuid/:id')
  getByUuid(@Param('id', ParseUUIDPipe) id: string) {
    return { id };
  }
}
```

## Custom Validation Errors

```typescript
import { BadRequestException, ValidationPipe } from '@nestjs/common';

app.useGlobalPipes(
  new ValidationPipe({
    exceptionFactory: (errors) => {
      const details = errors.map((err) => ({
        field: err.property,
        constraints: err.constraints,
      }));
      return new BadRequestException({ message: 'validation_error', details });
    },
  }),
);
```

## Tips

- Always validate query params and route params.
- Keep DTOs stable and versioned as your API grows.
- Avoid `any` in DTOs. Use enums, unions, and validation decorators.

---

[Previous: Services and Providers](./03-services-providers.md) | [Back to Index](./README.md) | [Next: Database Setup ->](./05-database-setup.md)
