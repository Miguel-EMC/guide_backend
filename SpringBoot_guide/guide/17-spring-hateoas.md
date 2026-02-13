# 17 - Spring HATEOAS

Spring HATEOAS helps you build hypermedia-driven REST APIs by adding links and affordances to representations. The latest release is 3.0.2.

## When to Use
- You want discoverable APIs with links and navigation
- You need HAL or other hypermedia formats
- You want to evolve APIs without breaking clients

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-hateoas</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-hateoas"
```

## Representation Example
```java
public record OrderDto(Long id, String status) {
}
```

```java
@Component
public class OrderModelAssembler implements RepresentationModelAssembler<OrderDto, EntityModel<OrderDto>> {

    @Override
    public EntityModel<OrderDto> toModel(OrderDto order) {
        return EntityModel.of(order,
            linkTo(methodOn(OrderController.class).getOrder(order.id())).withSelfRel(),
            linkTo(methodOn(OrderController.class).getOrders()).withRel("orders"));
    }
}
```

## Notes
- Pair with Spring MVC or Spring WebFlux controllers.
- Spring Data REST integrates with Spring HATEOAS out of the box.

## References
- [Spring HATEOAS project page](https://spring.io/projects/spring-hateoas)
- [Spring HATEOAS reference](https://docs.spring.io/spring-hateoas/reference/)
