# 17 - Spring HATEOAS

Spring HATEOAS is a library of APIs that you can use to create REST representations that follow the HATEOAS (Hypermedia as the Engine of Application State) principle. It provides a set of classes that allow you to easily add links to your REST representations.

To use Spring HATEOAS, you will need to add the `spring-boot-starter-hateoas` dependency to your project.

Once you have added the dependency, you can create a representation model by extending the `RepresentationModel` class.

Here is an example of a simple representation model:

```java
public class MyEntityModel extends RepresentationModel<MyEntityModel> {

    private final String name;

    public MyEntityModel(MyEntity entity) {
        this.name = entity.getName();
        add(linkTo(methodOn(MyController.class).myEntity(entity.getId())).withSelfRel());
    }
}
```
