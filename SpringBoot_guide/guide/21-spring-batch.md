# 21 - Spring Batch

Spring Batch is a framework for building robust batch processing jobs. The latest release is 5.1.2.

## When to Use
- Large-scale data processing or ETL jobs
- Scheduled or on-demand batch pipelines
- Retryable, restartable steps with checkpoints

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-batch"
```

## Job Example
```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Bean
    public Job importJob(JobRepository jobRepository, Step importStep) {
        return new JobBuilder("importJob", jobRepository)
            .start(importStep)
            .build();
    }

    @Bean
    public Step importStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("importStep", jobRepository)
            .<Customer, Customer>chunk(100, txManager)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .build();
    }
}
```

## Configuration Notes
- Ensure a database is available for the batch metadata tables.
- Use `spring.batch.jdbc.initialize-schema=always` for local development.

## References
- [Spring Batch project page](https://spring.io/projects/spring-batch)
- [Spring Batch reference](https://docs.spring.io/spring-batch/reference/)
