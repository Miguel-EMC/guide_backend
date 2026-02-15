# 14 - Authentication with JWT

This chapter implements email/password login with JWT access and refresh tokens.

## Goals

- Authenticate users securely
- Rotate refresh tokens safely
- Keep sessions revocable

## 1. Install Dependencies

```bash
npm install jsonwebtoken argon2
npm install -D @types/jsonwebtoken
```

## 2. Environment Variables

```env
JWT_ACCESS_SECRET=super-secret-access
JWT_ACCESS_TTL=15m
JWT_REFRESH_SECRET=super-secret-refresh
JWT_REFRESH_TTL=7d
JWT_ISSUER=express-api
JWT_AUDIENCE=express-clients
```

## 3. Hash Passwords

```typescript
import argon2 from 'argon2';

export async function hashPassword(password: string) {
  return argon2.hash(password, { type: argon2.argon2id });
}

export async function verifyPassword(hash: string, password: string) {
  return argon2.verify(hash, password);
}
```

## 4. Generate Tokens

```typescript
import jwt from 'jsonwebtoken';

export function signAccessToken(payload: object) {
  return jwt.sign(payload, process.env.JWT_ACCESS_SECRET as string, {
    expiresIn: process.env.JWT_ACCESS_TTL ?? '15m',
    issuer: process.env.JWT_ISSUER,
    audience: process.env.JWT_AUDIENCE,
  });
}

export function signRefreshToken(payload: object) {
  return jwt.sign(payload, process.env.JWT_REFRESH_SECRET as string, {
    expiresIn: process.env.JWT_REFRESH_TTL ?? '7d',
    issuer: process.env.JWT_ISSUER,
    audience: process.env.JWT_AUDIENCE,
  });
}
```

## 5. Store Refresh Tokens (Hashed)

Store a hashed refresh token and a token version in the database.

```typescript
import crypto from 'crypto';

export function hashRefreshToken(token: string) {
  return crypto.createHash('sha256').update(token).digest('hex');
}
```

```typescript
await prisma.user.update({
  where: { id: userId },
  data: {
    refreshTokenHash: hashRefreshToken(refreshToken),
    tokenVersion: { increment: 1 },
  },
});
```

## 6. Auth Middleware

```typescript
import type { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

export function auth(req: Request, res: Response, next: NextFunction) {
  const header = req.headers.authorization;
  const token = header?.replace('Bearer ', '');
  if (!token) return res.status(401).json({ message: 'missing_token' });

  try {
    const payload = jwt.verify(token, process.env.JWT_ACCESS_SECRET as string);
    (req as any).user = payload;
    return next();
  } catch {
    return res.status(401).json({ message: 'invalid_token' });
  }
}
```

## 7. Refresh Token Rotation

```typescript
export async function rotateRefreshToken(userId: number, oldToken: string) {
  const user = await prisma.user.findUnique({ where: { id: userId } });
  if (!user?.refreshTokenHash) return null;

  const ok = hashRefreshToken(oldToken) === user.refreshTokenHash;
  if (!ok) return null;

  const newToken = signRefreshToken({ sub: userId, ver: user.tokenVersion + 1 });

  await prisma.user.update({
    where: { id: userId },
    data: { refreshTokenHash: hashRefreshToken(newToken), tokenVersion: { increment: 1 } },
  });

  return newToken;
}
```

## 8. Cookie-Based Refresh Tokens

```typescript
res.cookie('refresh', refreshToken, {
  httpOnly: true,
  secure: true,
  sameSite: 'lax',
  path: '/auth/refresh',
  maxAge: 7 * 24 * 60 * 60 * 1000,
});
```

## 9. Logout

Invalidate refresh tokens by removing the stored hash.

```typescript
await prisma.user.update({ where: { id: userId }, data: { refreshTokenHash: null } });
```

## Tips

- Keep access tokens short-lived.
- Rotate refresh tokens on every use.
- Rate limit login and refresh endpoints.

---

[Previous: Validation with Zod](./13-validation.md) | [Back to Index](./README.md) | [Next: Testing ->](./15-testing-with-jest-and-supertest.md)
