# 15 - Spring Session

Spring Session is a framework that provides an API and implementations for managing a user's session information. It allows you to easily share session information between multiple application instances without being tied to an application container-specific solution.

To use Spring Session, you will need to add the `spring-session-core` dependency to your project, along with a dependency for the session store that you want to use (e.g., `spring-session-jdbc`, `spring-session-data-redis`, etc.).

Once you have added the dependencies, you can enable Spring Session by adding the `@EnableSpringHttpSession` annotation to your application.

Here is an example of how to enable Spring Session with a JDBC-based session store:

```java
@Configuration
@EnableJdbcHttpSession
public class HttpSessionConfig {

}
```
