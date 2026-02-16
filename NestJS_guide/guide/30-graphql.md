# GraphQL

NestJS provides first-class GraphQL support with two approaches: code-first and schema-first. This chapter covers Apollo integration, resolvers, mutations, subscriptions, and performance optimization.

## Goals

- Build a GraphQL API with NestJS
- Understand code-first vs schema-first
- Implement queries, mutations, and subscriptions
- Optimize with DataLoader

## Install Dependencies

```bash
npm install @nestjs/graphql @nestjs/apollo @apollo/server graphql
```

For subscriptions:

```bash
npm install graphql-subscriptions graphql-ws
```

## Code-First vs Schema-First

| Approach | Pros | Cons |
|----------|------|------|
| Code-First | TypeScript-native, auto-generated schema, better DX | Decorators can be verbose |
| Schema-First | SDL is standard, schema-driven development | Requires manual type sync |

This guide uses **code-first** as it integrates better with NestJS patterns.

## Module Setup

```typescript
// src/app.module.ts
import { Module } from '@nestjs/common';
import { GraphQLModule } from '@nestjs/graphql';
import { ApolloDriver, ApolloDriverConfig } from '@nestjs/apollo';
import { join } from 'path';

@Module({
  imports: [
    GraphQLModule.forRoot<ApolloDriverConfig>({
      driver: ApolloDriver,
      autoSchemaFile: join(process.cwd(), 'src/schema.gql'),
      sortSchema: true,
      playground: process.env.NODE_ENV !== 'production',
      introspection: process.env.NODE_ENV !== 'production',
    }),
  ],
})
export class AppModule {}
```

## Object Types

Define GraphQL types using decorators.

```typescript
// src/users/entities/user.entity.ts
import { ObjectType, Field, Int, ID } from '@nestjs/graphql';

@ObjectType()
export class User {
  @Field(() => ID)
  id: number;

  @Field()
  email: string;

  @Field()
  name: string;

  @Field(() => Int, { nullable: true })
  age?: number;

  @Field()
  createdAt: Date;

  // Exclude sensitive fields from GraphQL
  // passwordHash is NOT decorated, so it won't be exposed
  passwordHash: string;
}
```

```typescript
// src/posts/entities/post.entity.ts
import { ObjectType, Field, ID } from '@nestjs/graphql';
import { User } from '../../users/entities/user.entity';

@ObjectType()
export class Post {
  @Field(() => ID)
  id: number;

  @Field()
  title: string;

  @Field()
  content: string;

  @Field()
  published: boolean;

  @Field(() => User)
  author: User;

  @Field()
  createdAt: Date;
}
```

## Input Types

```typescript
// src/users/dto/create-user.input.ts
import { InputType, Field } from '@nestjs/graphql';
import { IsEmail, MinLength } from 'class-validator';

@InputType()
export class CreateUserInput {
  @Field()
  @IsEmail()
  email: string;

  @Field()
  @MinLength(2)
  name: string;

  @Field()
  @MinLength(8)
  password: string;
}
```

```typescript
// src/users/dto/update-user.input.ts
import { InputType, Field, PartialType, Int } from '@nestjs/graphql';
import { CreateUserInput } from './create-user.input';

@InputType()
export class UpdateUserInput extends PartialType(CreateUserInput) {
  @Field(() => Int)
  id: number;
}
```

## Resolvers

Resolvers are the GraphQL equivalent of controllers.

```typescript
// src/users/users.resolver.ts
import { Resolver, Query, Mutation, Args, Int, ResolveField, Parent } from '@nestjs/graphql';
import { User } from './entities/user.entity';
import { UsersService } from './users.service';
import { CreateUserInput } from './dto/create-user.input';
import { UpdateUserInput } from './dto/update-user.input';
import { Post } from '../posts/entities/post.entity';
import { PostsService } from '../posts/posts.service';

@Resolver(() => User)
export class UsersResolver {
  constructor(
    private readonly usersService: UsersService,
    private readonly postsService: PostsService,
  ) {}

  // Query: users
  @Query(() => [User], { name: 'users' })
  findAll() {
    return this.usersService.findAll();
  }

  // Query: user(id: 1)
  @Query(() => User, { name: 'user', nullable: true })
  findOne(@Args('id', { type: () => Int }) id: number) {
    return this.usersService.findOne(id);
  }

  // Mutation: createUser
  @Mutation(() => User)
  createUser(@Args('input') input: CreateUserInput) {
    return this.usersService.create(input);
  }

  // Mutation: updateUser
  @Mutation(() => User)
  updateUser(@Args('input') input: UpdateUserInput) {
    return this.usersService.update(input.id, input);
  }

  // Mutation: removeUser
  @Mutation(() => User)
  removeUser(@Args('id', { type: () => Int }) id: number) {
    return this.usersService.remove(id);
  }

  // Field resolver for nested posts
  @ResolveField(() => [Post])
  posts(@Parent() user: User) {
    return this.postsService.findByAuthor(user.id);
  }
}
```

## Posts Resolver

```typescript
// src/posts/posts.resolver.ts
import {
  Resolver,
  Query,
  Mutation,
  Args,
  Int,
  ResolveField,
  Parent,
} from '@nestjs/graphql';
import { Post } from './entities/post.entity';
import { PostsService } from './posts.service';
import { CreatePostInput } from './dto/create-post.input';
import { User } from '../users/entities/user.entity';
import { UsersService } from '../users/users.service';

@Resolver(() => Post)
export class PostsResolver {
  constructor(
    private readonly postsService: PostsService,
    private readonly usersService: UsersService,
  ) {}

  @Query(() => [Post], { name: 'posts' })
  findAll(
    @Args('published', { type: () => Boolean, nullable: true }) published?: boolean,
  ) {
    return this.postsService.findAll({ published });
  }

  @Query(() => Post, { name: 'post', nullable: true })
  findOne(@Args('id', { type: () => Int }) id: number) {
    return this.postsService.findOne(id);
  }

  @Mutation(() => Post)
  createPost(@Args('input') input: CreatePostInput) {
    return this.postsService.create(input);
  }

  @ResolveField(() => User)
  author(@Parent() post: Post) {
    return this.usersService.findOne(post.authorId);
  }
}
```

## Authentication

### GraphQL Context

```typescript
// src/app.module.ts
GraphQLModule.forRoot<ApolloDriverConfig>({
  driver: ApolloDriver,
  autoSchemaFile: true,
  context: ({ req, res }) => ({ req, res }),
}),
```

### Auth Guard for GraphQL

```typescript
// src/auth/guards/gql-auth.guard.ts
import { ExecutionContext, Injectable } from '@nestjs/common';
import { GqlExecutionContext } from '@nestjs/graphql';
import { AuthGuard } from '@nestjs/passport';

@Injectable()
export class GqlAuthGuard extends AuthGuard('jwt') {
  getRequest(context: ExecutionContext) {
    const ctx = GqlExecutionContext.create(context);
    return ctx.getContext().req;
  }
}
```

### Current User Decorator

```typescript
// src/auth/decorators/current-user.decorator.ts
import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import { GqlExecutionContext } from '@nestjs/graphql';

export const CurrentUser = createParamDecorator(
  (data: unknown, context: ExecutionContext) => {
    const ctx = GqlExecutionContext.create(context);
    return ctx.getContext().req.user;
  },
);
```

### Protected Resolver

```typescript
// src/posts/posts.resolver.ts
import { UseGuards } from '@nestjs/common';
import { GqlAuthGuard } from '../auth/guards/gql-auth.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';

@Resolver(() => Post)
export class PostsResolver {
  @Mutation(() => Post)
  @UseGuards(GqlAuthGuard)
  createPost(
    @Args('input') input: CreatePostInput,
    @CurrentUser() user: User,
  ) {
    return this.postsService.create({ ...input, authorId: user.id });
  }
}
```

## Subscriptions

Real-time updates with GraphQL subscriptions.

### Enable Subscriptions

```typescript
// src/app.module.ts
import { ApolloServerPluginLandingPageLocalDefault } from '@apollo/server/plugin/landingPage/default';

GraphQLModule.forRoot<ApolloDriverConfig>({
  driver: ApolloDriver,
  autoSchemaFile: true,
  subscriptions: {
    'graphql-ws': true,
    'subscriptions-transport-ws': false,
  },
  playground: false,
  plugins: [ApolloServerPluginLandingPageLocalDefault()],
}),
```

### PubSub Setup

```typescript
// src/pubsub/pubsub.module.ts
import { Global, Module } from '@nestjs/common';
import { PubSub } from 'graphql-subscriptions';

export const PUB_SUB = 'PUB_SUB';

@Global()
@Module({
  providers: [
    {
      provide: PUB_SUB,
      useValue: new PubSub(),
    },
  ],
  exports: [PUB_SUB],
})
export class PubSubModule {}
```

For production, use Redis PubSub:

```typescript
import { RedisPubSub } from 'graphql-redis-subscriptions';
import Redis from 'ioredis';

const options = {
  host: process.env.REDIS_HOST,
  port: parseInt(process.env.REDIS_PORT ?? '6379'),
};

@Global()
@Module({
  providers: [
    {
      provide: PUB_SUB,
      useValue: new RedisPubSub({
        publisher: new Redis(options),
        subscriber: new Redis(options),
      }),
    },
  ],
  exports: [PUB_SUB],
})
export class PubSubModule {}
```

### Subscription Resolver

```typescript
// src/posts/posts.resolver.ts
import { Resolver, Mutation, Subscription, Args } from '@nestjs/graphql';
import { Inject } from '@nestjs/common';
import { PubSub } from 'graphql-subscriptions';
import { PUB_SUB } from '../pubsub/pubsub.module';

const POST_CREATED = 'postCreated';
const POST_UPDATED = 'postUpdated';

@Resolver(() => Post)
export class PostsResolver {
  constructor(
    private readonly postsService: PostsService,
    @Inject(PUB_SUB) private readonly pubSub: PubSub,
  ) {}

  @Mutation(() => Post)
  async createPost(@Args('input') input: CreatePostInput) {
    const post = await this.postsService.create(input);
    this.pubSub.publish(POST_CREATED, { postCreated: post });
    return post;
  }

  @Mutation(() => Post)
  async updatePost(@Args('input') input: UpdatePostInput) {
    const post = await this.postsService.update(input.id, input);
    this.pubSub.publish(POST_UPDATED, { postUpdated: post });
    return post;
  }

  @Subscription(() => Post)
  postCreated() {
    return this.pubSub.asyncIterableIterator(POST_CREATED);
  }

  @Subscription(() => Post, {
    filter: (payload, variables) => {
      return payload.postUpdated.authorId === variables.authorId;
    },
  })
  postUpdated(@Args('authorId', { type: () => Int }) authorId: number) {
    return this.pubSub.asyncIterableIterator(POST_UPDATED);
  }
}
```

## DataLoader (N+1 Problem)

Batch and cache database calls to prevent N+1 queries.

### Install

```bash
npm install dataloader
```

### Create DataLoader

```typescript
// src/users/users.loader.ts
import { Injectable, Scope } from '@nestjs/common';
import DataLoader from 'dataloader';
import { UsersService } from './users.service';
import { User } from './entities/user.entity';

@Injectable({ scope: Scope.REQUEST })
export class UsersLoader {
  constructor(private readonly usersService: UsersService) {}

  readonly batchUsers = new DataLoader<number, User>(async (ids: number[]) => {
    const users = await this.usersService.findByIds([...ids]);
    const usersMap = new Map(users.map((user) => [user.id, user]));
    return ids.map((id) => usersMap.get(id) ?? null);
  });
}
```

### Use in Resolver

```typescript
// src/posts/posts.resolver.ts
@Resolver(() => Post)
export class PostsResolver {
  constructor(
    private readonly postsService: PostsService,
    private readonly usersLoader: UsersLoader,
  ) {}

  @ResolveField(() => User)
  author(@Parent() post: Post) {
    return this.usersLoader.batchUsers.load(post.authorId);
  }
}
```

## Pagination

### Cursor-Based Pagination

```typescript
// src/common/pagination/page-info.ts
import { ObjectType, Field } from '@nestjs/graphql';

@ObjectType()
export class PageInfo {
  @Field(() => String, { nullable: true })
  endCursor?: string;

  @Field()
  hasNextPage: boolean;
}
```

```typescript
// src/common/pagination/paginated.ts
import { Type } from '@nestjs/common';
import { ObjectType, Field } from '@nestjs/graphql';
import { PageInfo } from './page-info';

export function Paginated<T>(classRef: Type<T>) {
  @ObjectType(`${classRef.name}Edge`)
  abstract class EdgeType {
    @Field(() => String)
    cursor: string;

    @Field(() => classRef)
    node: T;
  }

  @ObjectType({ isAbstract: true })
  abstract class PaginatedType {
    @Field(() => [EdgeType])
    edges: EdgeType[];

    @Field(() => PageInfo)
    pageInfo: PageInfo;

    @Field()
    totalCount: number;
  }

  return PaginatedType;
}
```

```typescript
// src/posts/dto/paginated-posts.ts
import { ObjectType } from '@nestjs/graphql';
import { Paginated } from '../../common/pagination/paginated';
import { Post } from '../entities/post.entity';

@ObjectType()
export class PaginatedPosts extends Paginated(Post) {}
```

### Paginated Query

```typescript
@Query(() => PaginatedPosts)
async posts(
  @Args('first', { type: () => Int, defaultValue: 10 }) first: number,
  @Args('after', { type: () => String, nullable: true }) after?: string,
) {
  return this.postsService.findPaginated(first, after);
}
```

## Complexity and Depth Limiting

Prevent expensive queries.

```typescript
// src/app.module.ts
import { ApolloDriver, ApolloDriverConfig } from '@nestjs/apollo';
import depthLimit from 'graphql-depth-limit';
import { createComplexityLimitRule } from 'graphql-validation-complexity';

GraphQLModule.forRoot<ApolloDriverConfig>({
  driver: ApolloDriver,
  autoSchemaFile: true,
  validationRules: [
    depthLimit(7),
    createComplexityLimitRule(1000, {
      onCost: (cost) => console.log('Query cost:', cost),
    }),
  ],
}),
```

### Field Complexity

```typescript
@Field(() => [Post], { complexity: 10 })
posts: Post[];

@Field(() => [Post], {
  complexity: (options) => options.args.first * options.childComplexity,
})
paginatedPosts: Post[];
```

## Error Handling

### Custom Errors

```typescript
import { GraphQLError } from 'graphql';

throw new GraphQLError('Post not found', {
  extensions: {
    code: 'POST_NOT_FOUND',
    postId: id,
  },
});
```

### Error Formatting

```typescript
GraphQLModule.forRoot<ApolloDriverConfig>({
  driver: ApolloDriver,
  autoSchemaFile: true,
  formatError: (error) => {
    // Don't expose internal errors in production
    if (process.env.NODE_ENV === 'production') {
      if (error.extensions?.code === 'INTERNAL_SERVER_ERROR') {
        return new GraphQLError('Internal server error');
      }
    }
    return error;
  },
}),
```

## Schema-First Approach

If you prefer SDL:

```typescript
// src/app.module.ts
GraphQLModule.forRoot<ApolloDriverConfig>({
  driver: ApolloDriver,
  typePaths: ['./**/*.graphql'],
  definitions: {
    path: join(process.cwd(), 'src/graphql.ts'),
    outputAs: 'class',
  },
}),
```

```graphql
# src/users/users.graphql
type User {
  id: ID!
  email: String!
  name: String!
  posts: [Post!]!
}

type Query {
  users: [User!]!
  user(id: Int!): User
}

input CreateUserInput {
  email: String!
  name: String!
  password: String!
}

type Mutation {
  createUser(input: CreateUserInput!): User!
}
```

## Module Structure

```typescript
// src/posts/posts.module.ts
import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { PostsResolver } from './posts.resolver';
import { PostsService } from './posts.service';
import { Post } from './entities/post.entity';
import { UsersModule } from '../users/users.module';

@Module({
  imports: [TypeOrmModule.forFeature([Post]), UsersModule],
  providers: [PostsResolver, PostsService],
})
export class PostsModule {}
```

## Tips

- Use DataLoader for all nested field resolvers to prevent N+1.
- Implement query complexity limits to prevent abuse.
- Use cursor pagination for large datasets.
- Consider persisted queries for production.
- Add depth limiting to prevent deeply nested queries.
- Use `@nestjs/graphql` CLI plugin for automatic type generation.

## CLI Plugin

Add to `nest-cli.json`:

```json
{
  "compilerOptions": {
    "plugins": ["@nestjs/graphql"]
  }
}
```

This auto-generates `@Field()` decorators based on TypeScript types.

---

[Previous: Microservices](./29-microservices.md) | [Back to Index](./README.md) | [Next: OAuth2 and Social Auth ->](./31-oauth2-social-auth.md)
