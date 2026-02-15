# 13 - Data Validation with Zod

Zod provides runtime validation with TypeScript inference. Use it for request bodies, params, and queries.

## Goals

- Validate every request at the boundary
- Keep validation rules close to routes
- Return consistent error responses

## 1. Install Zod

```bash
npm install zod
```

## 2. Create a Validation Middleware

```typescript
// src/middleware/validate.ts
import type { Request, Response, NextFunction } from 'express';
import type { ZodSchema, ZodError } from 'zod';

function formatZodError(error: ZodError) {
  return error.issues.map((issue) => ({
    path: issue.path.join('.'),
    message: issue.message,
  }));
}

export function validate(schema: ZodSchema) {
  return (req: Request, res: Response, next: NextFunction) => {
    const result = schema.safeParse({
      body: req.body,
      params: req.params,
      query: req.query,
      headers: req.headers,
    });

    if (!result.success) {
      return res.status(400).json({
        message: 'validation_error',
        issues: formatZodError(result.error),
      });
    }

    return next();
  };
}
```

## 3. Define Schemas

```typescript
// src/modules/posts/posts.schema.ts
import { z } from 'zod';

export const createPostSchema = z.object({
  body: z
    .object({
      title: z.string().min(3).max(200),
      content: z.string().min(10),
      authorId: z.coerce.number().int(),
      tags: z.array(z.string().min(2)).default([]),
    })
    .strict(),
});

export const listPostsSchema = z.object({
  query: z.object({
    page: z.coerce.number().int().min(1).default(1),
    pageSize: z.coerce.number().int().min(1).max(100).default(20),
    search: z.string().trim().optional(),
  }),
});
```

## 4. Partial Updates

```typescript
export const updatePostSchema = z.object({
  params: z.object({ id: z.coerce.number().int() }),
  body: createPostSchema.shape.body.partial().strict(),
});
```

## 5. Use the Middleware

```typescript
// src/modules/posts/posts.routes.ts
import { validate } from '../../middleware/validate.js';
import { createPostSchema } from './posts.schema.js';

postsRouter.post('/', validate(createPostSchema), posts.create);
```

## 6. Refinements and Transforms

```typescript
const passwordSchema = z
  .string()
  .min(12)
  .refine((v) => /[A-Z]/.test(v), 'must include uppercase')
  .transform((v) => v.trim());
```

## 7. Infer Types

```typescript
import type { z } from 'zod';
import { createPostSchema } from './posts.schema.js';

type CreatePostInput = z.infer<typeof createPostSchema>['body'];
```

## 8. Validation for Files

Use `multer` for uploads and validate metadata.

```typescript
if (!['image/png', 'image/jpeg'].includes(file.mimetype)) {
  return res.status(400).json({ message: 'invalid_file_type' });
}
```

## Tips

- Validate params and query, not only body.
- Keep schemas close to routes.
- Use strict schemas to reject unknown fields.

---

[Previous: Building a CRUD API](./12-building-a-crud-api.md) | [Back to Index](./README.md) | [Next: Authentication with JWT ->](./14-authentication-with-jwt.md)
