# 03 - Dependency Injection and IoC

**Dependency Injection (DI)** is a design pattern that is used to implement IoC. It is a process whereby objects are given their dependencies at creation time by an external entity that is responsible for coordinating each object in the system.

In the Spring Framework, the IoC container is responsible for injecting the dependencies of your beans. You can provide the dependencies of your beans in the following ways:

*   **Constructor-based dependency injection:** The container injects the dependencies of your beans through the constructor of your class.
*   **Setter-based dependency injection:** The container injects the dependencies of your beans through the setter methods of your class.
*   **Field-based dependency injection:** The container injects the dependencies of your beans directly into the fields of your class.

Here is an example of constructor-based dependency injection:

```java
@Component
public class MyService {

    private final MyRepository repository;

    @Autowired
    public MyService(MyRepository repository) {
        this.repository = repository;
    }

    // ...
}
```
