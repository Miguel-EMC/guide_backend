# 22 - OpenAPI and API Versioning

This chapter documents an Express API with OpenAPI and explains versioning strategies.

## Goals

- Publish machine-readable API docs
- Generate Swagger UI or Redoc
- Plan for safe API evolution

## 1. Install Swagger Tools

```bash
npm install swagger-ui-express swagger-jsdoc
```

## 2. Create an OpenAPI Spec

```typescript
// src/docs/openapi.ts
import swaggerJsdoc from 'swagger-jsdoc';

export const openapiSpec = swaggerJsdoc({
  definition: {
    openapi: '3.1.0',
    info: {
      title: 'Express API',
      version: '1.0.0',
    },
    servers: [{ url: 'http://localhost:3000' }],
    components: {
      securitySchemes: {
        bearerAuth: { type: 'http', scheme: 'bearer' },
      },
    },
  },
  apis: ['./src/modules/**/*.routes.ts'],
});
```

## 3. Mount Swagger UI

```typescript
import swaggerUi from 'swagger-ui-express';
import { openapiSpec } from './docs/openapi.js';

app.use('/docs', swaggerUi.serve, swaggerUi.setup(openapiSpec));
```

## 4. Add JSDoc Annotations

```typescript
/**
 * @openapi
 * /posts:
 *   get:
 *     summary: List posts
 *     responses:
 *       200:
 *         description: OK
 */
postsRouter.get('/', posts.list);
```

## 5. Document Schemas

```typescript
/**
 * @openapi
 * components:
 *   schemas:
 *     Post:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         title:
 *           type: string
 */
```

## 6. Validate the Spec in CI

```bash
npm install -D @apidevtools/swagger-parser
```

```typescript
import SwaggerParser from '@apidevtools/swagger-parser';
import { openapiSpec } from './openapi.js';

await SwaggerParser.validate(openapiSpec);
```

## 7. Versioning Strategies

Common choices:

- URI versioning: `/v1/posts`
- Header versioning: `X-API-Version: 1`
- Media type versioning: `application/vnd.api+json;version=1`

## 8. Example: URI Versioning

```typescript
app.use('/v1/posts', postsRouter);
app.use('/v2/posts', postsV2Router);
```

## 9. Deprecation and Sunset

When you deprecate an API version:

- Announce a timeline.
- Add `Deprecation` and `Sunset` headers.
- Return warnings in responses.

## Tips

- Keep OpenAPI in source control.
- Validate docs on every PR.
- Document error responses and pagination.

---

[Previous: CI/CD](./21-ci-cd.md) | [Back to Index](./README.md) | [Next: OAuth/OIDC, RBAC, and Rate Limiting ->](./23-oauth-oidc-rbac-rate-limiting.md)
