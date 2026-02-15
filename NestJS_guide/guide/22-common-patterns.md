# Common Patterns

This chapter covers practical patterns you will use in production: pagination, filtering, soft deletes, transactions, and consistent API responses.

## Goals

- Build reusable pagination
- Implement filtering and sorting
- Handle soft deletes and transactions safely

## Pagination

```typescript
// src/common/dto/pagination.dto.ts
import { IsInt, IsOptional, Min } from 'class-validator';
import { Type } from 'class-transformer';

export class PaginationDto {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page = 1;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  pageSize = 20;
}
```

```typescript
// src/posts/posts.service.ts
async list({ page, pageSize }: PaginationDto) {
  const [items, total] = await this.postRepo.findAndCount({
    skip: (page - 1) * pageSize,
    take: pageSize,
    order: { createdAt: 'DESC' },
  });

  return {
    data: items,
    meta: { page, pageSize, total },
  };
}
```

## Filtering and Sorting

```typescript
// src/posts/dto/list-posts.dto.ts
import { IsIn, IsOptional, IsString } from 'class-validator';

export class ListPostsDto extends PaginationDto {
  @IsOptional()
  @IsString()
  q?: string;

  @IsOptional()
  @IsIn(['createdAt', 'title'])
  sortBy?: 'createdAt' | 'title' = 'createdAt';
}
```

## Soft Deletes

```typescript
await this.postRepo.softDelete(id);
```

```typescript
await this.postRepo.find({ withDeleted: true });
```

## Transactions

```typescript
import { DataSource } from 'typeorm';

await this.dataSource.transaction(async (manager) => {
  await manager.update(User, userId, { email: 'new@x.com' });
  await manager.insert(AuditLog, { userId, action: 'email_update' });
});
```

## Consistent API Responses

Define a consistent response shape via an interceptor.

```typescript
import { CallHandler, ExecutionContext, Injectable, NestInterceptor } from '@nestjs/common';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class ResponseInterceptor<T> implements NestInterceptor<T, { data: T; ts: string }> {
  intercept(_context: ExecutionContext, next: CallHandler): Observable<any> {
    return next.handle().pipe(map((data) => ({ data, ts: new Date().toISOString() })));
  }
}
```

## Tips

- Keep pagination defaults reasonable.
- Avoid dynamic column sorting without allow-lists.
- Wrap multi-write operations in a transaction.

---

[Previous: API Versioning](./21-api-versioning.md) | [Back to Index](./README.md) | [Next: NestJS 11 Features ->](./23-nestjs-11-features.md)
