# Authentication

This chapter implements local email/password login, JWT access tokens, and refresh tokens. It uses Passport strategies and demonstrates safe password storage.

## Goals

- Hash passwords safely
- Issue access and refresh tokens
- Protect routes with guards

## Install Dependencies

```bash
npm install @nestjs/passport passport passport-local passport-jwt
npm install @nestjs/jwt
npm install bcrypt
npm install -D @types/passport-local @types/passport-jwt @types/bcrypt
```

## Environment Variables

```env
JWT_ACCESS_SECRET=super-secret-access
JWT_ACCESS_TTL=15m
JWT_REFRESH_SECRET=super-secret-refresh
JWT_REFRESH_TTL=7d
```

## Auth Module

```typescript
// src/auth/auth.module.ts
import { Module } from '@nestjs/common';
import { PassportModule } from '@nestjs/passport';
import { JwtModule } from '@nestjs/jwt';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { UsersModule } from '../users/users.module';
import { LocalStrategy } from './strategies/local.strategy';
import { JwtStrategy } from './strategies/jwt.strategy';

@Module({
  imports: [
    UsersModule,
    PassportModule,
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        secret: config.get<string>('JWT_ACCESS_SECRET'),
        signOptions: { expiresIn: config.get('JWT_ACCESS_TTL', '15m') },
      }),
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService, LocalStrategy, JwtStrategy],
  exports: [AuthService],
})
export class AuthModule {}
```

## User Entity

Store only a password hash. Never store plain text passwords.

```typescript
// src/users/entities/user.entity.ts
import { Column, Entity } from 'typeorm';
import { BaseEntity } from '../../common/entities/base.entity';

@Entity('users')
export class User extends BaseEntity {
  @Column({ unique: true })
  email: string;

  @Column()
  passwordHash: string;

  @Column({ nullable: true })
  refreshTokenHash?: string | null;
}
```

## Auth Service

```typescript
// src/auth/auth.service.ts
import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import { UsersService } from '../users/users.service';
import { User } from '../users/entities/user.entity';

export interface JwtPayload {
  sub: number;
  email: string;
}

@Injectable()
export class AuthService {
  constructor(
    private readonly usersService: UsersService,
    private readonly jwtService: JwtService,
  ) {}

  async validateUser(email: string, password: string): Promise<User | null> {
    const user = await this.usersService.findByEmail(email);
    if (!user) return null;

    const ok = await bcrypt.compare(password, user.passwordHash);
    return ok ? user : null;
  }

  async login(user: User) {
    const payload: JwtPayload = { sub: user.id, email: user.email };
    const accessToken = this.jwtService.sign(payload);

    const refreshToken = this.jwtService.sign(payload, {
      secret: process.env.JWT_REFRESH_SECRET,
      expiresIn: process.env.JWT_REFRESH_TTL ?? '7d',
    });

    await this.usersService.setRefreshToken(user.id, refreshToken);

    return {
      user: { id: user.id, email: user.email },
      accessToken,
      refreshToken,
    };
  }

  async refresh(userId: number) {
    const user = await this.usersService.findOne(userId);
    if (!user) throw new UnauthorizedException('User not found');

    const payload: JwtPayload = { sub: user.id, email: user.email };
    const accessToken = this.jwtService.sign(payload);

    return { accessToken };
  }
}
```

## Users Service Token Storage

Hash refresh tokens before storage. Treat them like passwords.

```typescript
// src/users/users.service.ts
import * as bcrypt from 'bcrypt';

async setRefreshToken(userId: number, refreshToken: string) {
  const hash = await bcrypt.hash(refreshToken, 10);
  await this.userRepository.update(userId, { refreshTokenHash: hash });
}

async validateRefreshToken(userId: number, refreshToken: string) {
  const user = await this.findOne(userId);
  if (!user?.refreshTokenHash) return false;
  return bcrypt.compare(refreshToken, user.refreshTokenHash);
}
```

## Local Strategy

```typescript
// src/auth/strategies/local.strategy.ts
import { Injectable, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { Strategy } from 'passport-local';
import { AuthService } from '../auth.service';

@Injectable()
export class LocalStrategy extends PassportStrategy(Strategy) {
  constructor(private readonly authService: AuthService) {
    super({ usernameField: 'email' });
  }

  async validate(email: string, password: string) {
    const user = await this.authService.validateUser(email, password);
    if (!user) throw new UnauthorizedException('Invalid credentials');
    return user;
  }
}
```

## JWT Strategy

```typescript
// src/auth/strategies/jwt.strategy.ts
import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor() {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: process.env.JWT_ACCESS_SECRET,
    });
  }

  validate(payload: { sub: number; email: string }) {
    return { userId: payload.sub, email: payload.email };
  }
}
```

## Guards

```typescript
// src/auth/guards/local-auth.guard.ts
import { AuthGuard } from '@nestjs/passport';
import { Injectable } from '@nestjs/common';

@Injectable()
export class LocalAuthGuard extends AuthGuard('local') {}
```

```typescript
// src/auth/guards/jwt-auth.guard.ts
import { AuthGuard } from '@nestjs/passport';
import { Injectable } from '@nestjs/common';

@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {}
```

## Auth Controller

```typescript
// src/auth/auth.controller.ts
import { Body, Controller, Post, Req, UseGuards } from '@nestjs/common';
import { LocalAuthGuard } from './guards/local-auth.guard';
import { AuthService } from './auth.service';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @UseGuards(LocalAuthGuard)
  @Post('login')
  login(@Req() req: any) {
    return this.authService.login(req.user);
  }

  @Post('refresh')
  refresh(@Body() body: { userId: number; refreshToken: string }) {
    return this.authService.refresh(body.userId);
  }
}
```

## Tips

- Use HTTPS in production to protect tokens in transit.
- Prefer short access token TTLs and rotate refresh tokens.
- Store refresh tokens hashed and revoke them on logout.

---

[Previous: Error Handling](./07-error-handling.md) | [Back to Index](./README.md) | [Next: Guards and Interceptors ->](./09-guards-interceptors.md)
