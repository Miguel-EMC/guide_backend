# Swagger Documentation

NestJS integrates with Swagger (OpenAPI) via `@nestjs/swagger` to generate API docs automatically.

## Goals

- Generate OpenAPI docs from controllers and DTOs
- Add auth and versioning metadata
- Document pagination and error responses

## Install

```bash
npm install @nestjs/swagger
```

## Basic Setup

```typescript
// src/main.ts
import { NestFactory } from '@nestjs/core';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  const config = new DocumentBuilder()
    .setTitle('Blog API')
    .setDescription('REST API for blog management')
    .setVersion('1.0.0')
    .addBearerAuth()
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('docs', app, document, {
    swaggerOptions: { persistAuthorization: true },
  });

  await app.listen(3000);
}
bootstrap();
```

## Controller Metadata

```typescript
import { Controller, Get, Post, Body, Param } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiParam, ApiResponse, ApiTags } from '@nestjs/swagger';
import { CreatePostDto } from './dto/create-post.dto';
import { PostEntity } from './entities/post.entity';

@ApiTags('posts')
@Controller('posts')
export class PostsController {
  @Get(':id')
  @ApiOperation({ summary: 'Get a post by id' })
  @ApiParam({ name: 'id', type: Number })
  @ApiResponse({ status: 200, type: PostEntity })
  @ApiResponse({ status: 404, description: 'Post not found' })
  get(@Param('id') id: number) {
    return { id };
  }

  @Post()
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Create a post' })
  @ApiResponse({ status: 201, type: PostEntity })
  create(@Body() dto: CreatePostDto) {
    return dto;
  }
}
```

## DTO Metadata

```typescript
import { ApiProperty } from '@nestjs/swagger';
import { IsString } from 'class-validator';

export class CreatePostDto {
  @ApiProperty({ example: 'My first post' })
  @IsString()
  title: string;

  @ApiProperty({ example: 'Long form content...' })
  @IsString()
  body: string;
}
```

## Pagination Schema

You can document generic responses using `ApiExtraModels` and `getSchemaPath`.

```typescript
import { ApiExtraModels, ApiOkResponse, getSchemaPath } from '@nestjs/swagger';

class PaginationMeta {
  total: number;
  page: number;
  pageSize: number;
}

class PaginatedPostResponse {
  data: PostEntity[];
  meta: PaginationMeta;
}

@ApiExtraModels(PaginatedPostResponse)
@ApiOkResponse({
  schema: {
    allOf: [
      { $ref: getSchemaPath(PaginatedPostResponse) },
    ],
  },
})
@Get()
list() {}
```

## Protecting Docs

In production, protect Swagger behind auth or disable it entirely. A simple approach is to only enable it in non-production environments.

```typescript
if (process.env.NODE_ENV !== 'production') {
  SwaggerModule.setup('docs', app, document);
}
```

## Tips

- Document all auth requirements with `@ApiBearerAuth()`.
- Keep DTOs stable to avoid breaking API contracts.
- Use tags to group endpoints by domain.

---

[Previous: Guards and Interceptors](./09-guards-interceptors.md) | [Back to Index](./README.md) | [Next: Testing ->](./11-testing.md)
