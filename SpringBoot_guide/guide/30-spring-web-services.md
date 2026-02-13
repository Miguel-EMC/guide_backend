# 30 - Spring Web Services

Spring Web Services (Spring-WS) is focused on contract-first SOAP services and XML message handling. The current 4.1.x line is the latest.

## When to Use
- You need SOAP-based services
- You have WSDL-first contracts
- You require XML schema validation

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web-services</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-web-services"
```

## Endpoint Example
```java
@Endpoint
public class OrdersEndpoint {

    private static final String NAMESPACE_URI = "http://example.com/orders";

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetOrderRequest")
    @ResponsePayload
    public GetOrderResponse getOrder(@RequestPayload GetOrderRequest request) {
        return new GetOrderResponse();
    }
}
```

## WSDL Registration (Example)
```java
@Configuration
@EnableWs
public class WsConfig {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }
}
```

## Testing
Use `MockWebServiceClient` for endpoint tests.

## References
- [Spring Web Services project page](https://spring.io/projects/spring-ws)
- [Spring Web Services reference](https://docs.spring.io/spring-ws/docs/4.1.1/reference/html/)
