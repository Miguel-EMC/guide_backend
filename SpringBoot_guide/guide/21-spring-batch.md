# 21 - Spring Batch

Spring Batch is a lightweight, comprehensive batch framework designed to enable the development of robust batch applications vital for the daily operations of enterprise systems.

To use Spring Batch, you will need to add the `spring-boot-starter-batch` dependency to your project.

Once you have added the dependency, you can create a batch job by creating a `Job` bean.

Here is an example of a simple batch job that reads a CSV file, processes the data, and then writes it to a database:

```java
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Bean
    public Job myJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("myJob", jobRepository)
                .start(myStep(jobRepository, transactionManager))
                .build();
    }

    @Bean
    public Step myStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("myStep", jobRepository)
                .<MyData, MyData>chunk(10, transactionManager)
                .reader(myReader())
                .processor(myProcessor())
                .writer(myWriter())
                .build();
    }

    // ...
}
```
