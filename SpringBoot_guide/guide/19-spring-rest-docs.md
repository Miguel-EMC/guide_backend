# 19 - Spring REST Docs

Spring REST Docs generates API documentation snippets from your tests and combines them with hand-written docs. The latest release is 4.0.1.

## When to Use
- You want accurate docs that stay in sync with tests
- You prefer contract-first documentation
- You want HTML or PDF outputs via Asciidoctor

## Dependencies
### Maven (MockMvc)
```xml
<dependency>
  <groupId>org.springframework.restdocs</groupId>
  <artifactId>spring-restdocs-mockmvc</artifactId>
  <scope>test</scope>
</dependency>
```

### Gradle (WebTestClient)
```gradle
testImplementation "org.springframework.restdocs:spring-restdocs-webtestclient"
```

## MockMvc Example
```java
@WebMvcTest(OrdersController.class)
@AutoConfigureRestDocs
class OrdersControllerDocs {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getOrder() throws Exception {
        mockMvc.perform(get("/orders/{id}", 1))
            .andExpect(status().isOk())
            .andDo(document("orders-get"));
    }
}
```

## Notes
- Snippets are generated under `build/generated-snippets`.
- Use Asciidoctor to assemble final docs.

## References
- [Spring REST Docs project page](https://spring.io/projects/spring-restdocs)
- [Spring REST Docs reference](https://docs.spring.io/spring-restdocs/reference/)
