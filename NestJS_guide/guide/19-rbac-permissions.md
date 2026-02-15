# RBAC and Permissions

Role-based access control (RBAC) is a clean way to enforce permissions in NestJS. This chapter implements roles and permissions with guards and decorators.

## Goals

- Define roles and permissions
- Restrict access at route level
- Compose rules for complex policies

## Define Roles

```typescript
// src/auth/roles.ts
export enum Role {
  Admin = 'admin',
  Editor = 'editor',
  Viewer = 'viewer',
}
```

## Roles Decorator

```typescript
// src/common/decorators/roles.decorator.ts
import { SetMetadata } from '@nestjs/common';
import { Role } from '../../auth/roles';

export const ROLES_KEY = 'roles';
export const Roles = (...roles: Role[]) => SetMetadata(ROLES_KEY, roles);
```

## Roles Guard

```typescript
// src/common/guards/roles.guard.ts
import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { ROLES_KEY } from '../decorators/roles.decorator';
import { Role } from '../../auth/roles';

@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const required = this.reflector.getAllAndOverride<Role[]>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (!required) return true;

    const request = context.switchToHttp().getRequest();
    const user = request.user;
    return required.some((role) => user?.roles?.includes(role));
  }
}
```

## Applying RBAC

```typescript
import { Controller, Delete, Get, UseGuards } from '@nestjs/common';
import { Roles } from '../common/decorators/roles.decorator';
import { RolesGuard } from '../common/guards/roles.guard';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Role } from '../auth/roles';

@Controller('admin')
@UseGuards(JwtAuthGuard, RolesGuard)
export class AdminController {
  @Get('users')
  @Roles(Role.Admin)
  listUsers() {
    return [];
  }

  @Delete('users/:id')
  @Roles(Role.Admin)
  removeUser() {
    return { ok: true };
  }
}
```

## Permission Strings (Optional)

For large apps, represent permissions as strings and check them dynamically.

```typescript
export type Permission = 'post:create' | 'post:update' | 'post:delete';
```

```typescript
export const Permissions = (...permissions: Permission[]) =>
  SetMetadata('permissions', permissions);
```

## Tips

- Keep roles stable and documented.
- Combine RBAC with ownership checks for sensitive resources.
- Store roles in the JWT payload only if they change rarely.

---

[Previous: Queues and Jobs](./18-queues-jobs.md) | [Back to Index](./README.md) | [Next: Logging and Monitoring ->](./20-logging-monitoring.md)
