# Database Setup

NestJS is database-agnostic. You can use SQL or NoSQL with your preferred ORM or query builder.

## Goals

- Choose an ORM based on your use case
- Configure database connections with `ConfigModule`
- Prepare for migrations and production safety

## ORM Options

| Stack | Best For | Notes |
| --- | --- | --- |
| TypeORM | SQL + decorators | Great NestJS integration |
| Prisma | SQL + schema-first | Excellent migrations and DX |
| Sequelize | SQL + active record | Mature and stable |
| Mongoose | MongoDB | Official NestJS module |

This chapter uses PostgreSQL with TypeORM, then shows an alternative Prisma setup.

## Environment Variables

```env
# .env
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/mydb
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres
DATABASE_NAME=mydb
```

## Option A: TypeORM

### Install

```bash
npm install @nestjs/typeorm typeorm pg
npm install @nestjs/config
```

### Database Module

```typescript
// src/database/database.module.ts
import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ConfigModule, ConfigService } from '@nestjs/config';

@Module({
  imports: [
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        host: config.get('DATABASE_HOST'),
        port: config.get<number>('DATABASE_PORT'),
        username: config.get('DATABASE_USER'),
        password: config.get('DATABASE_PASSWORD'),
        database: config.get('DATABASE_NAME'),
        autoLoadEntities: true,
        synchronize: false,
        logging: config.get('NODE_ENV') === 'development',
      }),
    }),
  ],
})
export class DatabaseModule {}
```

### App Module

```typescript
// src/app.module.ts
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { DatabaseModule } from './database/database.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    DatabaseModule,
  ],
})
export class AppModule {}
```

### Migrations (TypeORM)

Use a `DataSource` for the CLI.

```typescript
// src/database/data-source.ts
import { DataSource } from 'typeorm';
import { config } from 'dotenv';

config();

export const AppDataSource = new DataSource({
  type: 'postgres',
  url: process.env.DATABASE_URL,
  entities: ['dist/**/*.entity.js'],
  migrations: ['dist/database/migrations/*.js'],
  synchronize: false,
});
```

Add scripts:

```json
{
  "scripts": {
    "typeorm": "typeorm-ts-node-commonjs -d dist/database/data-source.js",
    "migration:generate": "npm run build && npm run typeorm -- migration:generate src/database/migrations/init",
    "migration:run": "npm run build && npm run typeorm -- migration:run"
  }
}
```

## Option B: Prisma

### Install

```bash
npm install prisma @prisma/client
npx prisma init
```

### Prisma Service

```typescript
// src/prisma/prisma.service.ts
import { Injectable, OnModuleInit, OnModuleDestroy } from '@nestjs/common';
import { PrismaClient } from '@prisma/client';

@Injectable()
export class PrismaService extends PrismaClient implements OnModuleInit, OnModuleDestroy {
  async onModuleInit() {
    await this.$connect();
  }

  async onModuleDestroy() {
    await this.$disconnect();
  }
}
```

```typescript
// src/prisma/prisma.module.ts
import { Global, Module } from '@nestjs/common';
import { PrismaService } from './prisma.service';

@Global()
@Module({
  providers: [PrismaService],
  exports: [PrismaService],
})
export class PrismaModule {}
```

### Prisma Migrations

```bash
npx prisma migrate dev --name init
npx prisma generate
```

## Production Tips

- Never use `synchronize: true` in production.
- Run migrations during deploy, before serving traffic.
- Use connection pooling and set timeouts for cloud databases.

---

[Previous: Data Validation](./04-data-validation.md) | [Back to Index](./README.md) | [Next: Entities and Relationships ->](./06-entities-relationships.md)
