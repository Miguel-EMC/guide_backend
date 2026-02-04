# 33 - Spring gRPC

Spring gRPC is a project that provides support for gRPC. It allows you to create gRPC services and clients with Spring.

To use Spring gRPC, you will need to add the `grpc-spring-boot-starter` dependency to your project.

Once you have added the dependency, you can create a gRPC service by creating a class and annotating it with `@GrpcService`.

Here is an example of a simple gRPC service:

```java
@GrpcService
public class MyService extends MyServiceGrpc.MyServiceImplBase {

    @Override
    public void myMethod(MyRequest request, StreamObserver<MyResponse> responseObserver) {
        // ...
    }
}
```
