# OAuth2 and Social Authentication

This chapter implements OAuth2 authentication with popular providers like Google, GitHub, and Discord using Passport strategies.

## Goals

- Implement OAuth2 flows
- Add Google, GitHub, and Discord login
- Link social accounts to existing users
- Handle JWT tokens after OAuth

## Install Dependencies

```bash
npm install @nestjs/passport passport
npm install passport-google-oauth20 passport-github2 passport-discord
npm install -D @types/passport-google-oauth20 @types/passport-github2
```

## Environment Variables

```env
# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_CALLBACK_URL=http://localhost:3000/auth/google/callback

# GitHub OAuth
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
GITHUB_CALLBACK_URL=http://localhost:3000/auth/github/callback

# Discord OAuth
DISCORD_CLIENT_ID=your-discord-client-id
DISCORD_CLIENT_SECRET=your-discord-client-secret
DISCORD_CALLBACK_URL=http://localhost:3000/auth/discord/callback

# Frontend URL for redirects
FRONTEND_URL=http://localhost:5173
```

## User Entity with Social Providers

```typescript
// src/users/entities/user.entity.ts
import { Column, Entity, OneToMany } from 'typeorm';
import { BaseEntity } from '../../common/entities/base.entity';
import { SocialAccount } from './social-account.entity';

@Entity('users')
export class User extends BaseEntity {
  @Column({ unique: true })
  email: string;

  @Column({ nullable: true })
  passwordHash?: string;

  @Column()
  name: string;

  @Column({ nullable: true })
  avatar?: string;

  @Column({ default: false })
  emailVerified: boolean;

  @OneToMany(() => SocialAccount, (social) => social.user)
  socialAccounts: SocialAccount[];
}
```

```typescript
// src/users/entities/social-account.entity.ts
import { Column, Entity, ManyToOne, Unique } from 'typeorm';
import { BaseEntity } from '../../common/entities/base.entity';
import { User } from './user.entity';

export enum SocialProvider {
  GOOGLE = 'google',
  GITHUB = 'github',
  DISCORD = 'discord',
}

@Entity('social_accounts')
@Unique(['provider', 'providerId'])
export class SocialAccount extends BaseEntity {
  @Column({ type: 'enum', enum: SocialProvider })
  provider: SocialProvider;

  @Column()
  providerId: string;

  @Column({ nullable: true })
  accessToken?: string;

  @Column({ nullable: true })
  refreshToken?: string;

  @ManyToOne(() => User, (user) => user.socialAccounts, { onDelete: 'CASCADE' })
  user: User;

  @Column()
  userId: number;
}
```

## OAuth Service

```typescript
// src/auth/oauth.service.ts
import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from '../users/entities/user.entity';
import { SocialAccount, SocialProvider } from '../users/entities/social-account.entity';
import { AuthService } from './auth.service';

export interface OAuthProfile {
  provider: SocialProvider;
  providerId: string;
  email: string;
  name: string;
  avatar?: string;
  accessToken: string;
  refreshToken?: string;
}

@Injectable()
export class OAuthService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    @InjectRepository(SocialAccount)
    private readonly socialAccountRepository: Repository<SocialAccount>,
    private readonly authService: AuthService,
  ) {}

  async validateOAuthLogin(profile: OAuthProfile) {
    // Check if social account exists
    let socialAccount = await this.socialAccountRepository.findOne({
      where: {
        provider: profile.provider,
        providerId: profile.providerId,
      },
      relations: ['user'],
    });

    if (socialAccount) {
      // Update tokens
      socialAccount.accessToken = profile.accessToken;
      socialAccount.refreshToken = profile.refreshToken;
      await this.socialAccountRepository.save(socialAccount);
      return this.authService.login(socialAccount.user);
    }

    // Check if user with email exists
    let user = await this.userRepository.findOne({
      where: { email: profile.email },
    });

    if (!user) {
      // Create new user
      user = this.userRepository.create({
        email: profile.email,
        name: profile.name,
        avatar: profile.avatar,
        emailVerified: true,
      });
      user = await this.userRepository.save(user);
    }

    // Link social account
    socialAccount = this.socialAccountRepository.create({
      provider: profile.provider,
      providerId: profile.providerId,
      accessToken: profile.accessToken,
      refreshToken: profile.refreshToken,
      user,
    });
    await this.socialAccountRepository.save(socialAccount);

    return this.authService.login(user);
  }
}
```

## Google Strategy

```typescript
// src/auth/strategies/google.strategy.ts
import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { Strategy, VerifyCallback, Profile } from 'passport-google-oauth20';
import { OAuthService, OAuthProfile } from '../oauth.service';
import { SocialProvider } from '../../users/entities/social-account.entity';

@Injectable()
export class GoogleStrategy extends PassportStrategy(Strategy, 'google') {
  constructor(
    private readonly configService: ConfigService,
    private readonly oauthService: OAuthService,
  ) {
    super({
      clientID: configService.get('GOOGLE_CLIENT_ID'),
      clientSecret: configService.get('GOOGLE_CLIENT_SECRET'),
      callbackURL: configService.get('GOOGLE_CALLBACK_URL'),
      scope: ['email', 'profile'],
    });
  }

  async validate(
    accessToken: string,
    refreshToken: string,
    profile: Profile,
    done: VerifyCallback,
  ) {
    const oauthProfile: OAuthProfile = {
      provider: SocialProvider.GOOGLE,
      providerId: profile.id,
      email: profile.emails?.[0]?.value ?? '',
      name: profile.displayName,
      avatar: profile.photos?.[0]?.value,
      accessToken,
      refreshToken,
    };

    const result = await this.oauthService.validateOAuthLogin(oauthProfile);
    done(null, result);
  }
}
```

## GitHub Strategy

```typescript
// src/auth/strategies/github.strategy.ts
import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { Strategy, Profile } from 'passport-github2';
import { OAuthService, OAuthProfile } from '../oauth.service';
import { SocialProvider } from '../../users/entities/social-account.entity';

@Injectable()
export class GitHubStrategy extends PassportStrategy(Strategy, 'github') {
  constructor(
    private readonly configService: ConfigService,
    private readonly oauthService: OAuthService,
  ) {
    super({
      clientID: configService.get('GITHUB_CLIENT_ID'),
      clientSecret: configService.get('GITHUB_CLIENT_SECRET'),
      callbackURL: configService.get('GITHUB_CALLBACK_URL'),
      scope: ['user:email'],
    });
  }

  async validate(
    accessToken: string,
    refreshToken: string,
    profile: Profile,
    done: (err: any, user: any) => void,
  ) {
    const oauthProfile: OAuthProfile = {
      provider: SocialProvider.GITHUB,
      providerId: profile.id,
      email: profile.emails?.[0]?.value ?? '',
      name: profile.displayName || profile.username || '',
      avatar: profile.photos?.[0]?.value,
      accessToken,
      refreshToken,
    };

    const result = await this.oauthService.validateOAuthLogin(oauthProfile);
    done(null, result);
  }
}
```

## Discord Strategy

```typescript
// src/auth/strategies/discord.strategy.ts
import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { Strategy, Profile } from 'passport-discord';
import { OAuthService, OAuthProfile } from '../oauth.service';
import { SocialProvider } from '../../users/entities/social-account.entity';

@Injectable()
export class DiscordStrategy extends PassportStrategy(Strategy, 'discord') {
  constructor(
    private readonly configService: ConfigService,
    private readonly oauthService: OAuthService,
  ) {
    super({
      clientID: configService.get('DISCORD_CLIENT_ID'),
      clientSecret: configService.get('DISCORD_CLIENT_SECRET'),
      callbackURL: configService.get('DISCORD_CALLBACK_URL'),
      scope: ['identify', 'email'],
    });
  }

  async validate(
    accessToken: string,
    refreshToken: string,
    profile: Profile,
    done: (err: any, user: any) => void,
  ) {
    const avatar = profile.avatar
      ? `https://cdn.discordapp.com/avatars/${profile.id}/${profile.avatar}.png`
      : undefined;

    const oauthProfile: OAuthProfile = {
      provider: SocialProvider.DISCORD,
      providerId: profile.id,
      email: profile.email ?? '',
      name: profile.global_name || profile.username || '',
      avatar,
      accessToken,
      refreshToken,
    };

    const result = await this.oauthService.validateOAuthLogin(oauthProfile);
    done(null, result);
  }
}
```

## OAuth Guards

```typescript
// src/auth/guards/google-auth.guard.ts
import { Injectable } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

@Injectable()
export class GoogleAuthGuard extends AuthGuard('google') {}
```

```typescript
// src/auth/guards/github-auth.guard.ts
import { Injectable } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

@Injectable()
export class GitHubAuthGuard extends AuthGuard('github') {}
```

```typescript
// src/auth/guards/discord-auth.guard.ts
import { Injectable } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

@Injectable()
export class DiscordAuthGuard extends AuthGuard('discord') {}
```

## Auth Controller

```typescript
// src/auth/auth.controller.ts
import { Controller, Get, Req, Res, UseGuards } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Response } from 'express';
import { GoogleAuthGuard } from './guards/google-auth.guard';
import { GitHubAuthGuard } from './guards/github-auth.guard';
import { DiscordAuthGuard } from './guards/discord-auth.guard';

@Controller('auth')
export class AuthController {
  constructor(private readonly configService: ConfigService) {}

  // Google OAuth
  @Get('google')
  @UseGuards(GoogleAuthGuard)
  googleAuth() {
    // Guard redirects to Google
  }

  @Get('google/callback')
  @UseGuards(GoogleAuthGuard)
  googleCallback(@Req() req: any, @Res() res: Response) {
    return this.handleOAuthCallback(req, res);
  }

  // GitHub OAuth
  @Get('github')
  @UseGuards(GitHubAuthGuard)
  githubAuth() {
    // Guard redirects to GitHub
  }

  @Get('github/callback')
  @UseGuards(GitHubAuthGuard)
  githubCallback(@Req() req: any, @Res() res: Response) {
    return this.handleOAuthCallback(req, res);
  }

  // Discord OAuth
  @Get('discord')
  @UseGuards(DiscordAuthGuard)
  discordAuth() {
    // Guard redirects to Discord
  }

  @Get('discord/callback')
  @UseGuards(DiscordAuthGuard)
  discordCallback(@Req() req: any, @Res() res: Response) {
    return this.handleOAuthCallback(req, res);
  }

  private handleOAuthCallback(req: any, res: Response) {
    const { accessToken, refreshToken } = req.user;
    const frontendUrl = this.configService.get('FRONTEND_URL');

    // Redirect to frontend with tokens
    const params = new URLSearchParams({
      accessToken,
      refreshToken,
    });

    return res.redirect(`${frontendUrl}/auth/callback?${params.toString()}`);
  }
}
```

## Auth Module

```typescript
// src/auth/auth.module.ts
import { Module } from '@nestjs/common';
import { PassportModule } from '@nestjs/passport';
import { JwtModule } from '@nestjs/jwt';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AuthService } from './auth.service';
import { OAuthService } from './oauth.service';
import { AuthController } from './auth.controller';
import { User } from '../users/entities/user.entity';
import { SocialAccount } from '../users/entities/social-account.entity';
import { LocalStrategy } from './strategies/local.strategy';
import { JwtStrategy } from './strategies/jwt.strategy';
import { GoogleStrategy } from './strategies/google.strategy';
import { GitHubStrategy } from './strategies/github.strategy';
import { DiscordStrategy } from './strategies/discord.strategy';

@Module({
  imports: [
    TypeOrmModule.forFeature([User, SocialAccount]),
    PassportModule,
    JwtModule.registerAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        secret: config.get('JWT_ACCESS_SECRET'),
        signOptions: { expiresIn: config.get('JWT_ACCESS_TTL', '15m') },
      }),
    }),
  ],
  controllers: [AuthController],
  providers: [
    AuthService,
    OAuthService,
    LocalStrategy,
    JwtStrategy,
    GoogleStrategy,
    GitHubStrategy,
    DiscordStrategy,
  ],
  exports: [AuthService],
})
export class AuthModule {}
```

## Link Social Account to Existing User

Allow authenticated users to link additional social accounts.

```typescript
// src/auth/oauth.service.ts
async linkSocialAccount(userId: number, profile: OAuthProfile) {
  // Check if already linked
  const existing = await this.socialAccountRepository.findOne({
    where: {
      provider: profile.provider,
      providerId: profile.providerId,
    },
  });

  if (existing) {
    if (existing.userId !== userId) {
      throw new ConflictException(
        'This social account is already linked to another user',
      );
    }
    // Update tokens
    existing.accessToken = profile.accessToken;
    existing.refreshToken = profile.refreshToken;
    return this.socialAccountRepository.save(existing);
  }

  // Create new link
  const socialAccount = this.socialAccountRepository.create({
    provider: profile.provider,
    providerId: profile.providerId,
    accessToken: profile.accessToken,
    refreshToken: profile.refreshToken,
    userId,
  });

  return this.socialAccountRepository.save(socialAccount);
}

async unlinkSocialAccount(userId: number, provider: SocialProvider) {
  const user = await this.userRepository.findOne({
    where: { id: userId },
    relations: ['socialAccounts'],
  });

  // Ensure user has password or other social accounts
  if (!user.passwordHash && user.socialAccounts.length <= 1) {
    throw new BadRequestException(
      'Cannot unlink the only authentication method. Set a password first.',
    );
  }

  await this.socialAccountRepository.delete({
    userId,
    provider,
  });

  return { success: true };
}
```

## Link Controller

```typescript
// src/auth/auth.controller.ts
@Get('link/google')
@UseGuards(JwtAuthGuard, GoogleAuthGuard)
linkGoogle() {}

@Get('link/google/callback')
@UseGuards(JwtAuthGuard, GoogleAuthGuard)
async linkGoogleCallback(@Req() req: any) {
  // req.user contains existing user from JWT
  // oauth data is passed through session or state
  return { success: true, provider: 'google' };
}

@Delete('unlink/:provider')
@UseGuards(JwtAuthGuard)
async unlinkProvider(
  @CurrentUser() user: User,
  @Param('provider') provider: SocialProvider,
) {
  return this.oauthService.unlinkSocialAccount(user.id, provider);
}
```

## State Parameter for Security

Prevent CSRF attacks with state parameter.

```typescript
// src/auth/strategies/google.strategy.ts
import { v4 as uuid } from 'uuid';

@Injectable()
export class GoogleStrategy extends PassportStrategy(Strategy, 'google') {
  constructor(
    private readonly configService: ConfigService,
    private readonly oauthService: OAuthService,
  ) {
    super({
      clientID: configService.get('GOOGLE_CLIENT_ID'),
      clientSecret: configService.get('GOOGLE_CLIENT_SECRET'),
      callbackURL: configService.get('GOOGLE_CALLBACK_URL'),
      scope: ['email', 'profile'],
      state: true,
      passReqToCallback: true,
    });
  }

  async validate(
    req: Request,
    accessToken: string,
    refreshToken: string,
    profile: Profile,
    done: VerifyCallback,
  ) {
    // State is automatically validated by passport
    // Continue with profile validation
  }
}
```

## Frontend Integration

### Login Button

```html
<a href="/api/auth/google">
  <button>Continue with Google</button>
</a>

<a href="/api/auth/github">
  <button>Continue with GitHub</button>
</a>

<a href="/api/auth/discord">
  <button>Continue with Discord</button>
</a>
```

### Callback Handler (React)

```typescript
// src/pages/AuthCallback.tsx
import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function AuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setTokens } = useAuth();

  useEffect(() => {
    const accessToken = searchParams.get('accessToken');
    const refreshToken = searchParams.get('refreshToken');

    if (accessToken && refreshToken) {
      setTokens({ accessToken, refreshToken });
      navigate('/dashboard');
    } else {
      navigate('/login?error=oauth_failed');
    }
  }, [searchParams, setTokens, navigate]);

  return <div>Authenticating...</div>;
}
```

## Provider Configuration

### Google Cloud Console

1. Go to https://console.cloud.google.com/
2. Create a new project or select existing
3. Navigate to APIs & Services > Credentials
4. Create OAuth 2.0 Client ID
5. Set authorized redirect URIs

### GitHub Developer Settings

1. Go to https://github.com/settings/developers
2. Click "New OAuth App"
3. Set Authorization callback URL
4. Copy Client ID and generate Client Secret

### Discord Developer Portal

1. Go to https://discord.com/developers/applications
2. Create New Application
3. Go to OAuth2 section
4. Add redirect URL
5. Copy Client ID and Secret

## Error Handling

```typescript
// src/auth/filters/oauth-exception.filter.ts
import {
  ExceptionFilter,
  Catch,
  ArgumentsHost,
  HttpException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Response } from 'express';

@Catch()
export class OAuthExceptionFilter implements ExceptionFilter {
  constructor(private readonly configService: ConfigService) {}

  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();
    const frontendUrl = this.configService.get('FRONTEND_URL');

    let errorCode = 'oauth_error';
    if (exception instanceof HttpException) {
      const status = exception.getStatus();
      if (status === 401) errorCode = 'unauthorized';
      if (status === 409) errorCode = 'account_exists';
    }

    response.redirect(`${frontendUrl}/login?error=${errorCode}`);
  }
}
```

```typescript
// Use in controller
@UseFilters(OAuthExceptionFilter)
@Get('google/callback')
@UseGuards(GoogleAuthGuard)
googleCallback(@Req() req: any, @Res() res: Response) {
  return this.handleOAuthCallback(req, res);
}
```

## Tips

- Always use HTTPS in production for OAuth flows.
- Store refresh tokens securely (encrypted or hashed).
- Implement token rotation for refresh tokens.
- Consider adding account linking confirmation step.
- Log OAuth events for security monitoring.
- Handle edge cases like email conflicts gracefully.
- Test OAuth flows in staging before production.

---

[Previous: GraphQL](./30-graphql.md) | [Back to Index](./README.md) | [Next: OpenTelemetry ->](./32-opentelemetry.md)
