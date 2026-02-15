# Testing

NestJS ships with Jest and a testing module that makes unit, integration, and E2E testing straightforward.

## Goals

- Write isolated unit tests for services
- Run integration tests with a real module
- Validate full HTTP behavior with E2E tests

## Project Structure

```
src/
  users/
    users.service.spec.ts
    users.controller.spec.ts
test/
  app.e2e-spec.ts
  jest-e2e.json
```

## Unit Test Example

```typescript
// src/users/users.service.spec.ts
import { Test } from '@nestjs/testing';
import { UsersService } from './users.service';

describe('UsersService', () => {
  let service: UsersService;

  beforeEach(async () => {
    const moduleRef = await Test.createTestingModule({
      providers: [UsersService],
    }).compile();

    service = moduleRef.get(UsersService);
  });

  it('creates a user', async () => {
    const user = await service.create({ email: 'a@b.com', password: 'secret' });
    expect(user.email).toBe('a@b.com');
  });
});
```

## Mocking Providers

```typescript
import { getRepositoryToken } from '@nestjs/typeorm';

const mockRepo = {
  findOne: jest.fn(),
  save: jest.fn(),
};

const moduleRef = await Test.createTestingModule({
  providers: [
    UsersService,
    { provide: getRepositoryToken(User), useValue: mockRepo },
  ],
}).compile();
```

## Integration Test Example

```typescript
import { Test } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from '../src/app.module';

describe('UsersController (integration)', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleRef.createNestApplication();
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('GET /users returns 200', async () => {
    await request(app.getHttpServer()).get('/users').expect(200);
  });
});
```

## E2E Testing

```typescript
// test/app.e2e-spec.ts
import { Test } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from '../src/app.module';

describe('App (e2e)', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleRef.createNestApplication();
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('GET / returns 200', async () => {
    await request(app.getHttpServer()).get('/').expect(200);
  });
});
```

## Test Database Strategy

Use a separate database for tests. For SQL, use a dedicated schema or a disposable container.

Example environment override:

```env
NODE_ENV=test
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/myapp_test
```

## Tips

- Keep unit tests fast and isolated.
- Use integration tests for module wiring.
- Run E2E tests in CI against a real database.

---

[Previous: Swagger Documentation](./10-swagger-documentation.md) | [Back to Index](./README.md) | [Next: Deployment ->](./12-deployment.md)
