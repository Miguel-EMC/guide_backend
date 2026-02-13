# 26 - Spring Shell

Spring Shell lets you build interactive CLI applications using Spring. The latest release is 3.4.1.

## When to Use
- You need a CLI tool for internal automation
- You want interactive commands with options and help
- You need a Spring-powered command line UX

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.shell</groupId>
  <artifactId>spring-shell-starter</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.shell:spring-shell-starter"
```

## Command Example
```java
@ShellComponent
public class UserCommands {

    @ShellMethod("Creates a user.")
    public String createUser(@ShellOption String name) {
        return "Created " + name;
    }
}
```

## Notes
- Commands are auto-discovered via `@ShellComponent`.
- Use `@ShellOption` for defaults, validation, and help text.

## References
- [Spring Shell project page](https://spring.io/projects/spring-shell)
- [Spring Shell reference](https://docs.spring.io/spring-shell/reference/)
