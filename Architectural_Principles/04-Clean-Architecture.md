# 04 - Clean Architecture

Clean Architecture, proposed by Robert C. Martin (Uncle Bob), is a software design philosophy that separates the elements of a design into concentric layers. The key rule is the **Dependency Rule**: source code dependencies must point only inward, toward higher-level policies.

---

## The Dependency Rule

> Source code dependencies can only point inwards. Nothing in an inner circle can know anything at all about something in an outer circle.

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRAMEWORKS & DRIVERS                      │
│    (Web Framework, Database, External APIs, UI, Devices)         │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    INTERFACE ADAPTERS                        │ │
│  │      (Controllers, Gateways, Presenters, Repositories)       │ │
│  │  ┌─────────────────────────────────────────────────────────┐ │ │
│  │  │                  APPLICATION BUSINESS                    │ │ │
│  │  │                    (Use Cases)                           │ │ │
│  │  │  ┌─────────────────────────────────────────────────────┐ │ │ │
│  │  │  │              ENTERPRISE BUSINESS                     │ │ │ │
│  │  │  │        (Entities, Domain Rules)                      │ │ │ │
│  │  │  │                                                      │ │ │ │
│  │  │  └─────────────────────────────────────────────────────┘ │ │ │
│  │  └─────────────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘

                    Dependencies point INWARD →
```

---

## The Four Layers

### 1. Entities (Enterprise Business Rules)

The innermost layer contains enterprise-wide business rules. Entities encapsulate the most general and high-level rules. They are the least likely to change when something external changes.

```typescript
// entities/User.ts
export class User {
  private readonly id: UserId;
  private email: Email;
  private password: HashedPassword;
  private role: UserRole;
  private createdAt: Date;

  private constructor(
    id: UserId,
    email: Email,
    password: HashedPassword,
    role: UserRole,
  ) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.role = role;
    this.createdAt = new Date();
  }

  static create(email: Email, password: HashedPassword): User {
    return new User(
      UserId.generate(),
      email,
      password,
      UserRole.MEMBER,
    );
  }

  static reconstitute(
    id: UserId,
    email: Email,
    password: HashedPassword,
    role: UserRole,
    createdAt: Date,
  ): User {
    const user = new User(id, email, password, role);
    user.createdAt = createdAt;
    return user;
  }

  changeEmail(newEmail: Email): void {
    this.email = newEmail;
  }

  promoteToAdmin(): void {
    if (this.role === UserRole.ADMIN) {
      throw new Error('User is already an admin');
    }
    this.role = UserRole.ADMIN;
  }

  canAccessAdminPanel(): boolean {
    return this.role === UserRole.ADMIN;
  }

  getId(): UserId { return this.id; }
  getEmail(): Email { return this.email; }
  getRole(): UserRole { return this.role; }
}
```

```typescript
// entities/value-objects/Email.ts
export class Email {
  private readonly value: string;

  constructor(value: string) {
    if (!this.isValid(value)) {
      throw new InvalidEmailError(value);
    }
    this.value = value.toLowerCase();
  }

  private isValid(email: string): boolean {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
  }

  getValue(): string {
    return this.value;
  }

  equals(other: Email): boolean {
    return this.value === other.value;
  }
}
```

### 2. Use Cases (Application Business Rules)

Contains application-specific business rules. Use cases orchestrate the flow of data to and from entities, and direct those entities to use their enterprise-wide business rules.

```typescript
// use-cases/RegisterUser.ts
export interface RegisterUserInput {
  email: string;
  password: string;
}

export interface RegisterUserOutput {
  userId: string;
  email: string;
}

export class RegisterUserUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly passwordHasher: PasswordHasher,
    private readonly emailService: EmailService,
  ) {}

  async execute(input: RegisterUserInput): Promise<RegisterUserOutput> {
    // 1. Validate email is not taken
    const email = new Email(input.email);
    const existingUser = await this.userRepository.findByEmail(email);

    if (existingUser) {
      throw new UserAlreadyExistsError(email);
    }

    // 2. Hash password
    const hashedPassword = await this.passwordHasher.hash(input.password);

    // 3. Create user entity
    const user = User.create(email, hashedPassword);

    // 4. Persist
    await this.userRepository.save(user);

    // 5. Send welcome email
    await this.emailService.sendWelcomeEmail(email);

    // 6. Return output
    return {
      userId: user.getId().getValue(),
      email: user.getEmail().getValue(),
    };
  }
}
```

```typescript
// use-cases/AuthenticateUser.ts
export interface AuthenticateUserInput {
  email: string;
  password: string;
}

export interface AuthenticateUserOutput {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    email: string;
    role: string;
  };
}

export class AuthenticateUserUseCase {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly passwordHasher: PasswordHasher,
    private readonly tokenService: TokenService,
  ) {}

  async execute(input: AuthenticateUserInput): Promise<AuthenticateUserOutput> {
    const email = new Email(input.email);
    const user = await this.userRepository.findByEmail(email);

    if (!user) {
      throw new InvalidCredentialsError();
    }

    const passwordValid = await this.passwordHasher.verify(
      input.password,
      user.getPassword(),
    );

    if (!passwordValid) {
      throw new InvalidCredentialsError();
    }

    const accessToken = this.tokenService.generateAccessToken(user);
    const refreshToken = this.tokenService.generateRefreshToken(user);

    return {
      accessToken,
      refreshToken,
      user: {
        id: user.getId().getValue(),
        email: user.getEmail().getValue(),
        role: user.getRole(),
      },
    };
  }
}
```

### 3. Interface Adapters

Converts data from the format most convenient for use cases and entities to the format most convenient for external agencies like databases or the web.

```typescript
// adapters/controllers/UserController.ts
export class UserController {
  constructor(
    private readonly registerUser: RegisterUserUseCase,
    private readonly authenticateUser: AuthenticateUserUseCase,
  ) {}

  async register(request: HttpRequest): Promise<HttpResponse> {
    try {
      const result = await this.registerUser.execute({
        email: request.body.email,
        password: request.body.password,
      });

      return {
        statusCode: 201,
        body: {
          id: result.userId,
          email: result.email,
        },
      };
    } catch (error) {
      if (error instanceof UserAlreadyExistsError) {
        return { statusCode: 409, body: { error: error.message } };
      }
      if (error instanceof InvalidEmailError) {
        return { statusCode: 400, body: { error: error.message } };
      }
      throw error;
    }
  }

  async login(request: HttpRequest): Promise<HttpResponse> {
    try {
      const result = await this.authenticateUser.execute({
        email: request.body.email,
        password: request.body.password,
      });

      return {
        statusCode: 200,
        body: result,
      };
    } catch (error) {
      if (error instanceof InvalidCredentialsError) {
        return { statusCode: 401, body: { error: 'Invalid credentials' } };
      }
      throw error;
    }
  }
}
```

```typescript
// adapters/repositories/PostgresUserRepository.ts
export class PostgresUserRepository implements UserRepository {
  constructor(private readonly db: DatabaseConnection) {}

  async save(user: User): Promise<void> {
    await this.db.query(
      `INSERT INTO users (id, email, password, role, created_at)
       VALUES ($1, $2, $3, $4, $5)
       ON CONFLICT (id) DO UPDATE SET
         email = $2, password = $3, role = $4`,
      [
        user.getId().getValue(),
        user.getEmail().getValue(),
        user.getPassword().getValue(),
        user.getRole(),
        user.getCreatedAt(),
      ]
    );
  }

  async findByEmail(email: Email): Promise<User | null> {
    const result = await this.db.query(
      'SELECT * FROM users WHERE email = $1',
      [email.getValue()]
    );

    if (result.rows.length === 0) return null;

    const row = result.rows[0];
    return User.reconstitute(
      new UserId(row.id),
      new Email(row.email),
      new HashedPassword(row.password),
      row.role as UserRole,
      row.created_at,
    );
  }

  async findById(id: UserId): Promise<User | null> {
    const result = await this.db.query(
      'SELECT * FROM users WHERE id = $1',
      [id.getValue()]
    );

    if (result.rows.length === 0) return null;

    const row = result.rows[0];
    return User.reconstitute(
      new UserId(row.id),
      new Email(row.email),
      new HashedPassword(row.password),
      row.role as UserRole,
      row.created_at,
    );
  }
}
```

```typescript
// adapters/presenters/UserPresenter.ts
export class UserPresenter {
  static toViewModel(user: User): UserViewModel {
    return {
      id: user.getId().getValue(),
      email: user.getEmail().getValue(),
      role: this.formatRole(user.getRole()),
      memberSince: this.formatDate(user.getCreatedAt()),
    };
  }

  private static formatRole(role: UserRole): string {
    const roleNames: Record<UserRole, string> = {
      [UserRole.MEMBER]: 'Member',
      [UserRole.ADMIN]: 'Administrator',
    };
    return roleNames[role];
  }

  private static formatDate(date: Date): string {
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }
}
```

### 4. Frameworks & Drivers

The outermost layer. Contains frameworks and tools like the database, web framework, etc. This layer is where all the details go. We keep these things on the outside where they can do little harm.

```typescript
// frameworks/express/routes.ts
import express from 'express';
import { UserController } from '../../adapters/controllers/UserController';

export function createRouter(userController: UserController) {
  const router = express.Router();

  router.post('/register', async (req, res) => {
    const response = await userController.register({
      body: req.body,
      headers: req.headers,
    });
    res.status(response.statusCode).json(response.body);
  });

  router.post('/login', async (req, res) => {
    const response = await userController.login({
      body: req.body,
      headers: req.headers,
    });
    res.status(response.statusCode).json(response.body);
  });

  return router;
}
```

```typescript
// frameworks/database/PostgresConnection.ts
import { Pool } from 'pg';
import { DatabaseConnection } from '../../adapters/repositories/DatabaseConnection';

export class PostgresConnection implements DatabaseConnection {
  private pool: Pool;

  constructor(connectionString: string) {
    this.pool = new Pool({ connectionString });
  }

  async query(sql: string, params: any[]): Promise<QueryResult> {
    return this.pool.query(sql, params);
  }

  async transaction<T>(fn: (client: any) => Promise<T>): Promise<T> {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');
      const result = await fn(client);
      await client.query('COMMIT');
      return result;
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }
}
```

---

## Interfaces at Layer Boundaries

Use cases define interfaces that the outer layers must implement:

```typescript
// use-cases/interfaces/UserRepository.ts
export interface UserRepository {
  save(user: User): Promise<void>;
  findById(id: UserId): Promise<User | null>;
  findByEmail(email: Email): Promise<User | null>;
  delete(id: UserId): Promise<void>;
}
```

```typescript
// use-cases/interfaces/PasswordHasher.ts
export interface PasswordHasher {
  hash(plainPassword: string): Promise<HashedPassword>;
  verify(plainPassword: string, hashedPassword: HashedPassword): Promise<boolean>;
}
```

```typescript
// use-cases/interfaces/TokenService.ts
export interface TokenService {
  generateAccessToken(user: User): string;
  generateRefreshToken(user: User): string;
  verifyAccessToken(token: string): TokenPayload;
  verifyRefreshToken(token: string): TokenPayload;
}
```

```typescript
// use-cases/interfaces/EmailService.ts
export interface EmailService {
  sendWelcomeEmail(email: Email): Promise<void>;
  sendPasswordResetEmail(email: Email, token: string): Promise<void>;
}
```

---

## Project Structure

```
src/
├── domain/                          # Entities Layer
│   ├── entities/
│   │   ├── User.ts
│   │   ├── Order.ts
│   │   └── Product.ts
│   ├── value-objects/
│   │   ├── Email.ts
│   │   ├── UserId.ts
│   │   ├── Money.ts
│   │   └── HashedPassword.ts
│   ├── enums/
│   │   └── UserRole.ts
│   └── errors/
│       ├── InvalidEmailError.ts
│       └── InvalidOrderStateError.ts
│
├── application/                     # Use Cases Layer
│   ├── use-cases/
│   │   ├── user/
│   │   │   ├── RegisterUser.ts
│   │   │   ├── AuthenticateUser.ts
│   │   │   └── UpdateUserProfile.ts
│   │   └── order/
│   │       ├── CreateOrder.ts
│   │       └── CancelOrder.ts
│   ├── interfaces/                  # Output boundaries
│   │   ├── repositories/
│   │   │   ├── UserRepository.ts
│   │   │   └── OrderRepository.ts
│   │   └── services/
│   │       ├── PasswordHasher.ts
│   │       ├── TokenService.ts
│   │       └── EmailService.ts
│   └── errors/
│       ├── UserAlreadyExistsError.ts
│       └── InvalidCredentialsError.ts
│
├── adapters/                        # Interface Adapters Layer
│   ├── controllers/
│   │   ├── UserController.ts
│   │   └── OrderController.ts
│   ├── repositories/
│   │   ├── PostgresUserRepository.ts
│   │   └── PostgresOrderRepository.ts
│   ├── services/
│   │   ├── BcryptPasswordHasher.ts
│   │   ├── JwtTokenService.ts
│   │   └── SendGridEmailService.ts
│   ├── presenters/
│   │   └── UserPresenter.ts
│   └── mappers/
│       └── UserMapper.ts
│
├── frameworks/                      # Frameworks & Drivers Layer
│   ├── express/
│   │   ├── app.ts
│   │   ├── routes.ts
│   │   └── middleware/
│   │       └── authMiddleware.ts
│   ├── database/
│   │   ├── PostgresConnection.ts
│   │   └── migrations/
│   └── config/
│       └── env.ts
│
└── main.ts                          # Composition Root
```

---

## Composition Root (Dependency Injection)

```typescript
// main.ts
import express from 'express';
import { PostgresConnection } from './frameworks/database/PostgresConnection';
import { PostgresUserRepository } from './adapters/repositories/PostgresUserRepository';
import { BcryptPasswordHasher } from './adapters/services/BcryptPasswordHasher';
import { JwtTokenService } from './adapters/services/JwtTokenService';
import { SendGridEmailService } from './adapters/services/SendGridEmailService';
import { RegisterUserUseCase } from './application/use-cases/user/RegisterUser';
import { AuthenticateUserUseCase } from './application/use-cases/user/AuthenticateUser';
import { UserController } from './adapters/controllers/UserController';
import { createRouter } from './frameworks/express/routes';

async function main() {
  // Frameworks & Drivers
  const dbConnection = new PostgresConnection(process.env.DATABASE_URL);

  // Interface Adapters (Repositories & Services)
  const userRepository = new PostgresUserRepository(dbConnection);
  const passwordHasher = new BcryptPasswordHasher();
  const tokenService = new JwtTokenService(process.env.JWT_SECRET);
  const emailService = new SendGridEmailService(process.env.SENDGRID_API_KEY);

  // Use Cases
  const registerUser = new RegisterUserUseCase(
    userRepository,
    passwordHasher,
    emailService,
  );
  const authenticateUser = new AuthenticateUserUseCase(
    userRepository,
    passwordHasher,
    tokenService,
  );

  // Controllers
  const userController = new UserController(registerUser, authenticateUser);

  // Express App
  const app = express();
  app.use(express.json());
  app.use('/api', createRouter(userController));

  app.listen(3000, () => {
    console.log('Server running on port 3000');
  });
}

main();
```

---

## Clean Architecture vs Hexagonal Architecture

| Aspect | Clean Architecture | Hexagonal Architecture |
|--------|-------------------|------------------------|
| **Origin** | Robert C. Martin (2012) | Alistair Cockburn (2005) |
| **Layers** | 4 concentric circles | Inside (domain) vs Outside (adapters) |
| **Focus** | Dependency rule, layer separation | Ports and adapters |
| **Terminology** | Entities, Use Cases, Adapters, Frameworks | Domain, Ports, Adapters |
| **Similarity** | Both isolate business logic from infrastructure |

In practice, they are very similar and often used interchangeably. Clean Architecture can be seen as a more detailed specification of the same principles.

---

## Benefits

| Benefit | Description |
|---------|-------------|
| **Independent of Frameworks** | The architecture doesn't depend on any library or framework |
| **Testable** | Business rules can be tested without UI, database, or external services |
| **Independent of UI** | The UI can change easily without affecting business rules |
| **Independent of Database** | Business rules are not bound to any specific database |
| **Independent of External Agencies** | Business rules don't know anything about the outside world |

---

## Common Mistakes to Avoid

1. **Leaking Framework Dependencies**: Don't use framework decorators in entities
2. **Skipping Layers**: Don't call repositories directly from controllers
3. **Fat Use Cases**: Keep use cases focused on single responsibilities
4. **Anemic Entities**: Put business logic in entities, not just data
5. **Over-engineering**: Don't apply to simple CRUD applications
