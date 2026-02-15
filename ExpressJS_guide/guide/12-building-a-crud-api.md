# 12 - Building a CRUD API

This chapter builds a CRUD API for posts using Prisma.

## 1. Define Routes

```typescript
// src/modules/posts/posts.routes.ts
import { Router } from 'express';
import * as posts from './posts.controller.js';

export const postsRouter = Router();

postsRouter.get('/', posts.list);
postsRouter.get('/:id', posts.getById);
postsRouter.post('/', posts.create);
postsRouter.patch('/:id', posts.update);
postsRouter.delete('/:id', posts.remove);
```

## 2. Controller

```typescript
// src/modules/posts/posts.controller.ts
import type { Request, Response } from 'express';
import * as service from './posts.service.js';

export async function list(req: Request, res: Response) {
  const page = Number(req.query.page ?? 1);
  const pageSize = Number(req.query.pageSize ?? 20);
  const items = await service.listPosts(page, pageSize);
  res.json(items);
}

export async function getById(req: Request, res: Response) {
  const item = await service.getPost(Number(req.params.id));
  if (!item) return res.status(404).json({ message: 'not_found' });
  res.json(item);
}

export async function create(req: Request, res: Response) {
  const created = await service.createPost(req.body);
  res.status(201).json(created);
}

export async function update(req: Request, res: Response) {
  const updated = await service.updatePost(Number(req.params.id), req.body);
  res.json(updated);
}

export async function remove(req: Request, res: Response) {
  await service.deletePost(Number(req.params.id));
  res.status(204).send();
}
```

## 3. Service Layer

```typescript
// src/modules/posts/posts.service.ts
import { prisma } from '../../db/prisma.js';

export function listPosts(page: number, pageSize: number) {
  return prisma.post.findMany({
    skip: (page - 1) * pageSize,
    take: pageSize,
    orderBy: { createdAt: 'desc' },
  });
}

export function getPost(id: number) {
  return prisma.post.findUnique({ where: { id } });
}

export function createPost(data: { title: string; content: string; authorId: number }) {
  return prisma.post.create({ data });
}

export function updatePost(id: number, data: Partial<{ title: string; content: string }>) {
  return prisma.post.update({ where: { id }, data });
}

export function deletePost(id: number) {
  return prisma.post.delete({ where: { id } });
}

export function countPosts() {
  return prisma.post.count();
}
```

## 4. Wire the Routes

```typescript
// src/app.ts
import { postsRouter } from './modules/posts/posts.routes.js';

app.use('/posts', postsRouter);
```

## Tips

- Use pagination for list endpoints.
- Return 404 when resources do not exist.
- Validate input before hitting the database.

## Bonus: Include Metadata

Return pagination metadata for clients.

```typescript
export async function list(req: Request, res: Response) {
  const page = Number(req.query.page ?? 1);
  const pageSize = Number(req.query.pageSize ?? 20);
  const [items, total] = await Promise.all([
    service.listPosts(page, pageSize),
    service.countPosts(),
  ]);

  res.json({
    data: items,
    meta: { page, pageSize, total },
  });
}
```

---

[Previous: Structuring an Express App](./11-structuring-an-express-app.md) | [Back to Index](./README.md) | [Next: Validation with Zod ->](./13-validation.md)
