# Entities and Relationships

This chapter focuses on modeling relations with TypeORM in NestJS. The goal is to keep your data model expressive while staying safe and performant in production.

## Goals

- Model one-to-one, one-to-many, and many-to-many relations
- Avoid common pitfalls with cascades and eager loading
- Add indexes, constraints, and soft deletes

## Base Entity Pattern

Use a shared base entity to keep timestamps consistent.

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

## One-to-One

A user has a single profile. The owning side uses `@JoinColumn`.

```typescript
// src/users/entities/user.entity.ts
import { Column, Entity, OneToOne, JoinColumn } from 'typeorm';
import { BaseEntity } from '../../common/entities/base.entity';
import { Profile } from './profile.entity';

@Entity('users')
export class User extends BaseEntity {
  @Column({ unique: true })
  email: string;

  @Column()
  passwordHash: string;

  @OneToOne(() => Profile, (profile) => profile.user, {
    cascade: true,
  })
  @JoinColumn()
  profile: Profile;
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

## One-to-Many and Many-to-One

A user can author many posts. The many-to-one side stores the foreign key.

```typescript
// src/posts/entities/post.entity.ts
import { Column, Entity, ManyToOne, Index } from 'typeorm';
import { BaseEntity } from '../../common/entities/base.entity';
import { User } from '../../users/entities/user.entity';

@Entity('posts')
export class Post extends BaseEntity {
  @Column()
  title: string;

  @Column('text')
  body: string;

  @Index()
  @Column()
  authorId: number;

  @ManyToOne(() => User, (user) => user.posts, {
    onDelete: 'CASCADE',
  })
  author: User;
}
```

```typescript
// src/users/entities/user.entity.ts
import { OneToMany } from 'typeorm';
import { Post } from '../../posts/entities/post.entity';

@OneToMany(() => Post, (post) => post.author)
posts: Post[];
```

## Many-to-Many

Posts can have multiple tags. Use a junction table.

```typescript
// src/posts/entities/tag.entity.ts
import { Column, Entity, ManyToMany } from 'typeorm';
import { BaseEntity } from '../../common/entities/base.entity';
import { Post } from './post.entity';

@Entity('tags')
export class Tag extends BaseEntity {
  @Column({ unique: true })
  name: string;

  @ManyToMany(() => Post, (post) => post.tags)
  posts: Post[];
}
```

```typescript
// src/posts/entities/post.entity.ts
import { JoinTable, ManyToMany } from 'typeorm';
import { Tag } from './tag.entity';

@ManyToMany(() => Tag, (tag) => tag.posts, { cascade: true })
@JoinTable({ name: 'post_tags' })
tags: Tag[];
```

## Relation IDs for Performance

If you only need the foreign key, store it as a column. This avoids extra joins.

```typescript
@Index()
@Column()
authorId: number;
```

## Cascades and Orphans

Cascades are convenient but risky. Use them for simple child records only.

```typescript
@OneToMany(() => Address, (address) => address.user, {
  cascade: ['insert', 'update'],
  orphanedRowAction: 'delete',
})
addresses: Address[];
```

Keep cascade limited to `insert` and `update`. Avoid `remove` when the child is shared.

## Eager vs Lazy Loading

- Eager loading is convenient but can cause large queries.
- Lazy loading hides IO behind property access and complicates testing.

Prefer explicit `relations` in queries so you know what is loaded.

```typescript
return this.userRepository.findOne({
  where: { id },
  relations: ['profile', 'posts'],
});
```

## Indexes and Constraints

```typescript
import { Index, Unique } from 'typeorm';

@Entity('users')
@Unique(['email'])
@Index(['createdAt'])
export class User extends BaseEntity {
  @Column()
  email: string;
}
```

## Soft Deletes

Soft delete keeps records but marks them as deleted.

```typescript
await this.userRepository.softDelete(id);
```

To query including deleted rows, use:

```typescript
await this.userRepository.find({ withDeleted: true });
```

## Tips

- Store foreign keys explicitly for faster queries.
- Keep relations explicit in queries to control data loading.
- Use database constraints instead of trusting only validation.

---

[Previous: Database Setup](./05-database-setup.md) | [Back to Index](./README.md) | [Next: Error Handling ->](./07-error-handling.md)
