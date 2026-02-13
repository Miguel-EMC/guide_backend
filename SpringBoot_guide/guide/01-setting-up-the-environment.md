# 01 - Setting Up the Environment

Spring Boot 4 requires Java 17 or later. Install the JDK, a build tool, and an IDE to get started.

## Java
Install JDK 17+ (Eclipse Temurin, Oracle, Azul, etc.).

Verify:
```bash
java -version
```

Expected output (example):
```
openjdk version "17.0.10"
OpenJDK Runtime Environment
OpenJDK 64-Bit Server VM
```

## Build Tools
Use Maven or Gradle. Spring Boot 4 requires Maven 3.6.3+ and Gradle 8.14+ (or Gradle 9.x). The wrappers (`./mvnw`, `./gradlew`) are recommended for consistent builds.

Verify Maven:
```bash
mvn -version
```

Verify Gradle:
```bash
gradle -version
```

## IDEs
Pick one with Spring support:
- IntelliJ IDEA
- Spring Tools Suite (STS)
- Visual Studio Code with Spring extensions

## Spring Initializr
Generate a project at:
```
https://start.spring.io
```

Select:
- Project: Maven or Gradle
- Language: Java
- Spring Boot: 4.0.x
- Dependencies: Web, Data JPA, Security, etc.

## References
- [Spring Boot system requirements](https://docs.spring.io/spring-boot/reference/system-requirements.html)
- [Spring Initializr](https://start.spring.io/)
