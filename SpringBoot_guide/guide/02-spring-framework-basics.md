# 02 - Spring Framework Basics

The Spring Framework is a powerful and versatile framework for building Java applications. It provides a comprehensive programming and configuration model for modern Java-based enterprise applications - on any kind of deployment platform.

At its core, the Spring Framework provides a container, known as the **Inversion of Control (IoC) container**, that is responsible for managing the lifecycle of your application's objects. These objects are known as **beans**.

## The IoC Container

The IoC container is the heart of the Spring Framework. It is responsible for creating, configuring, and managing the beans in your application. The container gets its instructions on what objects to instantiate, configure, and assemble by reading configuration metadata. The configuration metadata can be represented in XML, Java annotations, or Java code.

The two main types of IoC containers in the Spring Framework are:

*   **`BeanFactory`:** This is the simplest type of container, and it provides basic support for dependency injection.
*   **`ApplicationContext`:** This is a more advanced container that provides a number of additional features, such as internationalization, event propagation, and application lifecycle events.

In a Spring Boot application, an `ApplicationContext` is created for you automatically.

## Beans

A bean is an object that is instantiated, assembled, and otherwise managed by a Spring IoC container. These beans are created from the configuration metadata that you provide to the container.

You can define a bean in a number of ways, but the most common way is to use the `@Component` annotation.

```java
@Component
public class MyBean {

    public void doSomething() {
        // ...
    }
}
```

The `@Component` annotation is a generic stereotype annotation that can be used to define any type of bean. There are also a number of more specific stereotype annotations that you can use, such as:

*   **`@Repository`:** This annotation is used to define a bean that is responsible for data access.
*   **`@Service`:** This annotation is used to define a bean that contains business logic.
*   **`@Controller`:** This annotation is used to define a bean that is responsible for handling web requests.

## Inversion of Control

Inversion of Control (IoC) is a design principle in which the control of object creation and object linking is removed from the application and given to a container or framework. In the Spring Framework, the IoC container is responsible for creating and linking the beans in your application.

This is the "inversion" part of the name: instead of the application code being responsible for creating and managing its own dependencies, the container is responsible for it. This leads to a more loosely coupled system that is easier to test and maintain.