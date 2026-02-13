# 32 - Spring Cloud Data Flow

Spring Cloud Data Flow is a toolkit for building, deploying, and managing data integration pipelines (streaming and batch). The latest open-source version is 2.11.5. Broadcom has announced that the open-source project is no longer maintained.

## When to Use
- Streaming pipelines using Spring Cloud Stream
- Batch workflows using Spring Cloud Task
- Centralized lifecycle management for pipeline apps

## Architecture Overview
- Pipelines are composed of Spring Boot apps built with Spring Cloud Stream or Spring Cloud Task.
- The Data Flow server uses Spring Cloud Deployer to deploy applications to platforms like Kubernetes and Cloud Foundry.

## Basic Stream Example
```
http --server.port=9000 | log
```

## References
- [Spring Cloud Data Flow project page](https://spring.io/projects/spring-cloud-dataflow)
- [Spring Cloud Data Flow open source update](https://spring.io/blog/2024/05/08/an-update-on-spring-cloud-data-flow)
