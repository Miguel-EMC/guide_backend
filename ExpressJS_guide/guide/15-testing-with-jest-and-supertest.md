# 15 - Testing with Jest and Supertest

This chapter covers unit and integration tests using Jest and Supertest.

## Goals

- Test services quickly with unit tests
- Verify routes with integration tests
- Keep the test database isolated

## 1. Install Dependencies

```bash
npm install -D jest ts-jest @types/jest supertest @types/supertest
```

## 2. Configure Jest for TypeScript (ESM)

```typescript
// jest.config.ts
import type { Config } from 'jest';

const config: Config = {
  testEnvironment: 'node',
  extensionsToTreatAsEsm: ['.ts'],
  transform: {
    '^.+\\.ts$': [
      'ts-jest',
      { useESM: true, tsconfig: './tsconfig.json' },
    ],
  },
  moduleNameMapper: {
    '^(.*)\\.js$': '$1',
  },
  setupFilesAfterEnv: ['./test/setup.ts'],
};

export default config;
```

Update package.json:

```json
{
  "scripts": {
    "test": "node --experimental-vm-modules node_modules/jest/bin/jest.js"
  }
}
```

## 3. Test App Setup

```typescript
// test/setup.ts
process.env.NODE_ENV = 'test';
process.env.LOG_LEVEL = 'silent';
```

Keep `app.ts` free of `listen` so tests can import it directly.

## 4. Integration Test with Supertest

```typescript
// test/health.spec.ts
import request from 'supertest';
import { app } from '../src/app.js';

describe('GET /health', () => {
  it('returns ok', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
  });
});
```

## 5. Unit Test Example

```typescript
// src/modules/posts/posts.service.spec.ts
import { listPosts } from './posts.service.js';

describe('posts service', () => {
  it('returns a list', async () => {
    const posts = await listPosts();
    expect(Array.isArray(posts)).toBe(true);
  });
});
```

## 6. Test Database Strategy

Use a separate database or schema for tests.

```env
NODE_ENV=test
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/express_test
```

Reset state in `beforeEach` to keep tests isolated.

## 7. Authenticated Requests

```typescript
const token = 'test-token';
const res = await request(app)
  .get('/protected')
  .set('Authorization', `Bearer ${token}`);
```

## 8. Mock External Services

Use `nock` or `msw` to mock HTTP calls.

```bash
npm install -D nock
```

## 9. Coverage

```bash
npm test -- --coverage
```

## 10. CI Stability Tips

- Use `--runInBand` for slow CI runners.
- Use `--detectOpenHandles` when tests hang.
- Set timeouts for external calls.

## Tips

- Keep unit tests fast and isolated.
- Reset the database between tests.
- Use integration tests for request-level behavior.

---

[Previous: Authentication with JWT](./14-authentication-with-jwt.md) | [Back to Index](./README.md) | [Next: Deployment with Docker ->](./16-deployment-docker.md)
