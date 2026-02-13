# 31 - Spring Cloud

Spring Cloud provides tools to build distributed systems, including configuration management, service discovery, routing, circuit breakers, messaging, and more.

## Release Trains and Compatibility
Spring Cloud ships in release trains. The current train is **2025.1.1 (Oakwood)** and it supports **Spring Boot 4.0.2**. Always align your Spring Cloud version with the compatible Spring Boot line.

## Dependency Management (Recommended)
Use the Spring Cloud BOM or Spring Initializr to keep versions aligned.

### Maven
```xml
<properties>
  <spring-cloud.version>2025.1.1</spring-cloud.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>${spring-cloud.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### Gradle
```gradle
ext {
  set("springCloudVersion", "2025.1.1")
}

dependencies {
  implementation platform("org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}")
}
```

## Common Starters
- Configuration: `spring-cloud-starter-config`
- Service discovery: `spring-cloud-starter-netflix-eureka-client`
- API gateway: `spring-cloud-starter-gateway`
- Declarative HTTP: `spring-cloud-starter-openfeign`
- Circuit breaker: `spring-cloud-starter-circuitbreaker-resilience4j`
- Messaging: `spring-cloud-starter-stream-kafka` or `spring-cloud-starter-stream-rabbit`
- Kubernetes integration: `spring-cloud-starter-kubernetes-client`
- Secrets: `spring-cloud-starter-vault-config`

## References
- [Spring Cloud project page](https://spring.io/projects/spring-cloud)
- [Spring Cloud release train reference](https://docs.spring.io/spring-cloud/release/reference/html/)
- [Spring Cloud 2025.1.1 release notes](https://spring.io/blog/2025/12/19/spring-cloud-2025-1-1-is-available)
- [Spring Boot 4.0.2 release notes](https://spring.io/blog/2025/12/11/spring-boot-4-0-2-available-now)
