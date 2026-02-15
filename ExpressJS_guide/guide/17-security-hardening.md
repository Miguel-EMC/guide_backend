# 17 - Security Hardening

Security is a set of defaults and habits. This chapter provides a production baseline for Express 5 APIs.

## Goals

- Reduce attack surface
- Prevent common web vulnerabilities
- Build repeatable security controls

## 1. Start with a Threat Model

Write down what you protect and from whom.

- Assets: user data, tokens, admin endpoints
- Entry points: HTTP routes, webhooks, file uploads
- Trust boundaries: public internet, internal services, databases
- Attacker goals: data exfiltration, account takeover, abuse

## 2. Keep the Runtime and Dependencies Current

- Run on Node LTS in production.
- Prefer Express 5 for modern routing and error handling.
- Use lockfiles and `npm ci` for deterministic installs.
- Run `npm audit` and dependency scanning in CI.

## 3. Security Headers (Helmet)

```typescript
import helmet from 'helmet';

app.use(
  helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        objectSrc: ["'none'"],
        frameAncestors: ["'none'"],
        upgradeInsecureRequests: [],
      },
    },
    referrerPolicy: { policy: 'no-referrer' },
    crossOriginResourcePolicy: { policy: 'same-site' },
  }),
);
```

## 4. Enforce HTTPS and HSTS

```typescript
app.use((req, res, next) => {
  const proto = req.header('x-forwarded-proto');
  if (req.secure || proto === 'https') return next();
  return res.redirect(301, `https://${req.headers.host}${req.originalUrl}`);
});

app.use(
  helmet.hsts({
    maxAge: 15552000,
    includeSubDomains: true,
    preload: true,
  }),
);
```

## 5. CORS with an Allowlist

```typescript
import cors from 'cors';

const allowlist = new Set(['https://app.example.com']);

app.use(
  cors({
    origin(origin, cb) {
      if (!origin || allowlist.has(origin)) return cb(null, true);
      return cb(new Error('Not allowed by CORS'));
    },
    credentials: true,
  }),
);
```

## 6. Request Size Limits and Parsing

```typescript
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: false, limit: '1mb' }));
```

Reject oversized requests early to reduce abuse and memory pressure.

## 7. Rate Limiting with Redis

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

## 8. Password Hashing and Auth Hardening

Prefer `argon2id` or bcrypt with a strong cost.

```bash
npm install argon2
```

```typescript
import argon2 from 'argon2';

export async function hashPassword(password: string) {
  return argon2.hash(password, { type: argon2.argon2id });
}

export async function verifyPassword(hash: string, password: string) {
  return argon2.verify(hash, password);
}
```

Add MFA for admin roles and rotate refresh tokens on use.

## 9. Cookies and CSRF

- Use HttpOnly, Secure, and SameSite cookies.
- If you use cookies for auth, add CSRF tokens.
- If you use Authorization headers, disable cookie auth.

```typescript
res.cookie('refresh', token, {
  httpOnly: true,
  secure: true,
  sameSite: 'lax',
  path: '/auth/refresh',
  maxAge: 7 * 24 * 60 * 60 * 1000,
});
```

## 10. Trust Proxy in Production

Set this when running behind a reverse proxy or load balancer.

```typescript
app.set('trust proxy', 1);
```

## 11. File Upload Hygiene

- Use strict size limits.
- Store in object storage, not the app filesystem.
- Scan uploads for malware.

```typescript
import multer from 'multer';

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 },
});
```

## 12. SSRF and Outbound Requests

- Allowlist external domains.
- Use timeouts and small payload limits.
- Block internal IP ranges when calling third parties.

## 13. Secrets Management

- Use environment variables or a secrets manager.
- Never log tokens or secrets.
- Rotate keys and revoke leaked tokens quickly.

## 14. Supply Chain and CI Controls

- Pin dependencies with lockfiles.
- Use `npm ci` in CI.
- Block installs with unexpected scripts if you do not need them.

## 15. Security Checklist

- HTTPS enforced
- Helmet and CSP configured
- Rate limits in place
- Input validated everywhere
- Secrets stored outside code
- CI scanning enabled

---

[Previous: Deployment with Docker](./16-deployment-docker.md) | [Back to Index](./README.md) | [Next: Observability and Logging ->](./18-observability-logging.md)
