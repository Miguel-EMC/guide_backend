# 33 - Spring gRPC

Spring gRPC provides Spring Boot integration for gRPC servers and clients. The 1.0.x line supports Spring Boot 4.0.x, and the reference docs track the current development line.

## When to Use
- High-performance RPC communication
- Strongly typed APIs with protobuf contracts
- Streaming APIs with backpressure

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.grpc</groupId>
  <artifactId>spring-grpc-spring-boot-starter</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.grpc:spring-grpc-spring-boot-starter"
```

Optional modules:
- `spring-grpc-server-spring-boot-starter`
- `spring-grpc-client-spring-boot-starter`
- `spring-grpc-test`

## Service Example
```java
@Service
public class OrdersGrpcService extends OrdersGrpc.OrdersImplBase {

    @Override
    public void getOrder(GetOrderRequest request, StreamObserver<GetOrderResponse> responseObserver) {
        GetOrderResponse response = GetOrderResponse.newBuilder()
            .setId(request.getId())
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

## Client Example
```java
@Service
public class OrdersClient {

    private final OrdersGrpc.OrdersBlockingStub stub;

    public OrdersClient(GrpcChannelFactory channels) {
        this.stub = OrdersGrpc.newBlockingStub(channels.createChannel("local"));
    }
}
```

## Client Configuration
```yaml
spring:
  grpc:
    client:
      channels:
        local:
          address: 0.0.0.0:9090
```

## Testing
Use `spring-grpc-test` for in-process server testing and channel setup.

## Notes
- Spring gRPC is still evolving; check the reference docs for the current development line.

## References
- [Spring gRPC reference](https://docs.spring.io/spring-grpc/reference/)
- [Spring gRPC source repository](https://github.com/spring-projects/spring-grpc)
