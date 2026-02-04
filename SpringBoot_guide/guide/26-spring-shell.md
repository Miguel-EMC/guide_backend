# 26 - Spring Shell

Spring Shell is a project that allows you to create a shell (or command line) application with Spring. It provides a simple and extensible way to build your own custom commands.

To use Spring Shell, you will need to add the `spring-shell-starter` dependency to your project.

Once you have added the dependency, you can create a command by creating a class and annotating it with `@ShellComponent` and `@ShellMethod`.

Here is an example of a simple command that prints a greeting:

```java
@ShellComponent
public class MyCommands {

    @ShellMethod("Prints a greeting.")
    public String hello(@ShellOption(defaultValue = "World") String name) {
        return "Hello, " + name + "!";
    }
}
```
