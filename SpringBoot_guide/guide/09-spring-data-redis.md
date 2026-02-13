# 09 - Spring Data Redis

Spring Data Redis provides low-level and high-level abstractions for Redis, including `RedisTemplate`, reactive APIs, repository support, and features like Pub/Sub, Sentinel, and Cluster. The latest stable version is 4.0.2.

## When to Use
- Caching or short-lived data
- Pub/Sub messaging
- Rate limiting and counters
- Session storage

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-data-redis"
```

For reactive applications, also add:
```gradle
implementation "org.springframework.boot:spring-boot-starter-data-redis-reactive"
```

## Configuration
Spring Boot auto-configures a `RedisConnectionFactory`, `StringRedisTemplate`, and `RedisTemplate` when Redis is on the classpath.

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      username: default
      password: secret
      ssl:
        enabled: false
```

For advanced topologies, configure one of:
- `RedisStandaloneConfiguration`
- `RedisSentinelConfiguration`
- `RedisClusterConfiguration`

Connection pooling can be enabled by adding `commons-pool2` to the classpath.

## Template Example
```java
@Service
public class RateLimitService {
    private final StringRedisTemplate stringRedisTemplate;

    public RateLimitService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long increment(String key, Duration ttl) {
        Long value = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, ttl);
        return value == null ? 0 : value;
    }
}
```

## Repository Example
```java
@RedisHash("orders")
public class Order {
    @Id
    private String id;
    private String status;
}
```

```java
@Repository
public interface OrderRepository extends CrudRepository<Order, String> {
    List<Order> findByStatus(String status);
}
```

## Notes and Pitfalls
- Choose a serialization strategy up front (JSON, JDK, or custom).
- Set TTLs explicitly for ephemeral data.
- Avoid using Redis as a primary system of record.

## References
- [Spring Data Redis project page](https://spring.io/projects/spring-data-redis)
- [Spring Data Redis reference](https://docs.spring.io/spring-data/redis/reference/)
- [Spring Boot Redis auto-configuration reference](https://docs.spring.io/spring-boot/reference/data/nosql.html#data.nosql.redis)
