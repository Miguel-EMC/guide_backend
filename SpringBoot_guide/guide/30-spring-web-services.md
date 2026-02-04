# 30 - Spring Web Services

Spring Web Services is a product of the Spring community focused on creating document-driven Web services. It provides a contract-first approach to developing SOAP-based web services.

To use Spring Web Services, you will need to add the `spring-boot-starter-web-services` dependency to your project.

Once you have added the dependency, you can create a web service endpoint by creating a class and annotating it with `@Endpoint`.

Here is an example of a simple web service endpoint:

```java
@Endpoint
public class MyEndpoint {

    private static final String NAMESPACE_URI = "http://my-namespace.com";

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "myRequest")
    @ResponsePayload
    public MyResponse myMethod(@RequestPayload MyRequest request) {
        // ...
    }
}
```
