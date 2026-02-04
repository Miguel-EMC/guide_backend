# 19 - Spring REST Docs

Spring REST Docs is a tool that helps you document your RESTful services. It combines handwritten documentation with auto-generated snippets produced with the Spring MVC Test framework.

To use Spring REST Docs, you will need to add the `spring-restdocs-mockmvc` dependency to your project.

Once you have added the dependency, you can create a test that generates documentation snippets.

Here is an example of a test that generates a documentation snippet for a simple REST API:

```java
@WebMvcTest(MyController.class)
@AutoConfigureRestDocs
public class MyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldReturnMyEntity() throws Exception {
        this.mockMvc.perform(get("/my-entity/1"))
                .andExpect(status().isOk())
                .andDo(document("my-entity"));
    }
}
```
