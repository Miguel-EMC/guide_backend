# 23 - OAuth/OIDC, RBAC, and Rate Limiting

This chapter integrates OAuth/OIDC authentication, advanced RBAC, and rate limiting backed by Redis.

## Goals

- Authenticate users with OAuth/OIDC
- Enforce role and permission checks
- Rate limit across multiple API instances

## 1. OAuth/OIDC with OpenID Client

```bash
npm install openid-client jose
```

```typescript
// src/auth/oidc.ts
import { Issuer, generators } from 'openid-client';

export async function createOidcClient() {
  const issuer = await Issuer.discover(process.env.OIDC_ISSUER_URL as string);
  return new issuer.Client({
    client_id: process.env.OIDC_CLIENT_ID as string,
    client_secret: process.env.OIDC_CLIENT_SECRET as string,
    redirect_uris: [process.env.OIDC_REDIRECT_URI as string],
    response_types: ['code'],
  });
}

export function createAuthUrl(client: any) {
  const codeVerifier = generators.codeVerifier();
  const codeChallenge = generators.codeChallenge(codeVerifier);

  const url = client.authorizationUrl({
    scope: 'openid profile email',
    code_challenge: codeChallenge,
    code_challenge_method: 'S256',
  });

  return { url, codeVerifier };
}
```

## 2. Verify ID Tokens

```typescript
import { jwtVerify, createRemoteJWKSet } from 'jose';

const jwks = createRemoteJWKSet(new URL(process.env.OIDC_JWKS_URL as string));

export async function verifyIdToken(token: string) {
  const { payload } = await jwtVerify(token, jwks, {
    issuer: process.env.OIDC_ISSUER_URL,
    audience: process.env.OIDC_CLIENT_ID,
  });
  return payload;
}
```

## 3. RBAC with Permissions

Define permissions as strings and map them to roles.

```typescript
// src/auth/permissions.ts
export type Permission =
  | 'post:create'
  | 'post:update'
  | 'post:delete'
  | 'user:read'
  | 'user:write';

export const rolePermissions: Record<string, Permission[]> = {
  admin: ['post:create', 'post:update', 'post:delete', 'user:read', 'user:write'],
  editor: ['post:create', 'post:update'],
  viewer: ['user:read'],
};
```

```typescript
// src/middleware/authorize.ts
import type { Request, Response, NextFunction } from 'express';
import { rolePermissions, type Permission } from '../auth/permissions.js';

export function requireAll(required: Permission[]) {
  return (req: Request, res: Response, next: NextFunction) => {
    const role = (req as any).user?.role;
    const allowed = rolePermissions[role] ?? [];
    const ok = required.every((perm) => allowed.includes(perm));
    if (!ok) return res.status(403).json({ message: 'forbidden' });
    return next();
  };
}

export function requireAny(required: Permission[]) {
  return (req: Request, res: Response, next: NextFunction) => {
    const role = (req as any).user?.role;
    const allowed = rolePermissions[role] ?? [];
    const ok = required.some((perm) => allowed.includes(perm));
    if (!ok) return res.status(403).json({ message: 'forbidden' });
    return next();
  };
}
```

## 4. Rate Limiting with Redis Store

```bash
npm install express-rate-limit rate-limit-redis ioredis
```

```typescript
import rateLimit from 'express-rate-limit';
import RedisStore from 'rate-limit-redis';
import Redis from 'ioredis';

const redis = new Redis(process.env.REDIS_URL as string);

const apiLimiter = rateLimit({
  windowMs: 60_000,
  max: 600,
  standardHeaders: true,
  legacyHeaders: false,
  store: new RedisStore({
    sendCommand: (...args: string[]) => redis.call(...args),
  }),
});

const authLimiter = rateLimit({
  windowMs: 60_000,
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  store: new RedisStore({
    sendCommand: (...args: string[]) => redis.call(...args),
  }),
});

app.use('/api', apiLimiter);
app.use('/auth', authLimiter);
```

## Tips

- Prefer Authorization Code + PKCE for OIDC.
- Keep roles stable and version permissions carefully.
- Rate limit login and token endpoints more aggressively.

---

[Previous: OpenAPI and Versioning](./22-openapi-and-versioning.md) | [Back to Index](./README.md) | [Next: SRE and Operations ->](./24-sre-operations.md)
