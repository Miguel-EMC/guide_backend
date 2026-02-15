# Project: Blog API

This chapter walks you through a production-style Blog API using NestJS, TypeORM, and JWT auth. It is intentionally opinionated so you can move quickly and still end up with a clean architecture.

## Goals

- Build a modular, testable REST API
- Model users, posts, and categories with relations
- Implement auth, validation, and pagination
- Prepare the project for production

## 1. Scaffold the Project

```bash
nest new blog-api
cd blog-api
```

Create modules using the CLI:

```bash
nest g module users
nest g module posts
nest g module categories
nest g module auth
nest g module common
```

## 2. Set Up the Database

Use PostgreSQL with TypeORM (see chapter 05 for full setup). Add a shared base entity:

```typescript
// src/common/entities/base.entity.ts
import {
  PrimaryGeneratedColumn,
  CreateDateColumn,
  UpdateDateColumn,
  DeleteDateColumn,
} from 'typeorm';

export abstract class BaseEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  @DeleteDateColumn()
  deletedAt?: Date | null;
}
```

## 3. Entities

```typescript
// src/users/entities/user.entity.ts
import { Column, Entity, OneToMany, OneToOne, JoinColumn } from 'typeorm';
import { BaseEntity } from '../../common/entities/base.entity';
import { Profile } from './profile.entity';
import { Post } from '../../posts/entities/post.entity';

@Entity('users')
export class User extends BaseEntity {
  @Column({ unique: true })
  email: string;

  @Column()
  passwordHash: string;

  @OneToOne(() => Profile, (profile) => profile.user, { cascade: true })
  @JoinColumn()
  profile: Profile;

  @OneToMany(() => Post, (post) => post.author)
  posts: Post[];
}
```

```typescript
// src/users/entities/profile.entity.ts
import { Column, Entity, OneToOne } from 'typeorm';
import { BaseEntity } from '../../common/entities/base.entity';
import { User } from './user.entity';

@Entity('profiles')
export class Profile extends BaseEntity {
  @Column()
  firstName: string;

  @Column()
  lastName: string;

  @Column({ nullable: true })
  avatarUrl?: string;

  @OneToOne(() => User, (user) => user.profile)
  user: User;
}
```

```typescript
// src/posts/entities/post.entity.ts
import { Column, Entity, ManyToOne, ManyToMany, JoinTable, Index } from 'typeorm';
import { BaseEntity } from '../../common/entities/base.entity';
import { User } from '../../users/entities/user.entity';
import { Category } from '../../categories/entities/category.entity';

@Entity('posts')
export class Post extends BaseEntity {
  @Column()
  title: string;

  @Column('text')
  body: string;

  @Index()
  @Column()
  authorId: number;

  @ManyToOne(() => User, (user) => user.posts, { onDelete: 'CASCADE' })
  author: User;

  @ManyToMany(() => Category, (category) => category.posts, { cascade: true })
  @JoinTable({ name: 'post_categories' })
  categories: Category[];
}
```

```typescript
// src/categories/entities/category.entity.ts
import { Column, Entity, ManyToMany } from 'typeorm';
import { BaseEntity } from '../../common/entities/base.entity';
import { Post } from '../../posts/entities/post.entity';

@Entity('categories')
export class Category extends BaseEntity {
  @Column({ unique: true })
  name: string;

  @ManyToMany(() => Post, (post) => post.categories)
  posts: Post[];
}
```

## 4. DTOs and Validation

```typescript
// src/posts/dto/create-post.dto.ts
import { IsArray, IsString, MinLength } from 'class-validator';

export class CreatePostDto {
  @IsString()
  @MinLength(3)
  title: string;

  @IsString()
  @MinLength(10)
  body: string;

  @IsArray()
  categoryIds: number[];
}
```

## 5. Services

```typescript
// src/posts/posts.service.ts
import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import { Post } from './entities/post.entity';
import { Category } from '../categories/entities/category.entity';
import { CreatePostDto } from './dto/create-post.dto';

@Injectable()
export class PostsService {
  constructor(
    @InjectRepository(Post) private readonly postRepo: Repository<Post>,
    @InjectRepository(Category) private readonly categoryRepo: Repository<Category>,
  ) {}

  async create(authorId: number, dto: CreatePostDto) {
    const categories = await this.categoryRepo.findBy({ id: In(dto.categoryIds) });
    const post = this.postRepo.create({
      title: dto.title,
      body: dto.body,
      authorId,
      categories,
    });
    return this.postRepo.save(post);
  }

  async findOne(id: number) {
    const post = await this.postRepo.findOne({ where: { id }, relations: ['author', 'categories'] });
    if (!post) throw new NotFoundException('Post not found');
    return post;
  }
}
```

## 6. Controllers

```typescript
// src/posts/posts.controller.ts
import { Body, Controller, Get, Param, Post } from '@nestjs/common';
import { PostsService } from './posts.service';
import { CreatePostDto } from './dto/create-post.dto';

@Controller('posts')
export class PostsController {
  constructor(private readonly postsService: PostsService) {}

  @Post()
  create(@Body() dto: CreatePostDto) {
    const authorId = 1; // Replace with auth context
    return this.postsService.create(authorId, dto);
  }

  @Get(':id')
  get(@Param('id') id: number) {
    return this.postsService.findOne(Number(id));
  }
}
```

## 7. Authentication

Use JWT auth from chapter 08. For production, store refresh tokens hashed and rotate them.

## 8. Pagination Pattern

```typescript
// src/common/dto/pagination.dto.ts
import { IsInt, IsOptional, Min } from 'class-validator';
import { Type } from 'class-transformer';

export class PaginationDto {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page = 1;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  pageSize = 20;
}
```

## 9. Production Checklist (Quick)

- Use migrations only, never `synchronize: true`.
- Validate env vars at startup.
- Set up health checks and logging.
- Run tests in CI and deploy with containers.

---

[Previous: Deployment](./12-deployment.md) | [Back to Index](./README.md) | [Next: Caching ->](./14-caching.md)
