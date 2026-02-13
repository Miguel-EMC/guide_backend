# 29 - Spring Web Flow

Spring Web Flow provides a framework for defining and managing conversational web flows. The current major line is 4.0.0.

## When to Use
- Complex multi-step web forms
- Wizards and long-lived user interactions
- Flows that need explicit state transitions

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.webflow</groupId>
  <artifactId>spring-webflow</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.webflow:spring-webflow"
```

## Flow Definition (XML)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<flow xmlns="http://www.springframework.org/schema/webflow"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.springframework.org/schema/webflow
                          http://www.springframework.org/schema/webflow/spring-webflow.xsd">

    <view-state id="step1">
        <transition on="next" to="step2"/>
    </view-state>

    <view-state id="step2">
        <transition on="previous" to="step1"/>
    </view-state>

    <end-state id="done"/>
</flow>
```

## Notes
- Web Flow is commonly paired with Spring MVC.
- Flows can also be defined in Java configuration for dynamic use cases.

## References
- [Spring Web Flow project page](https://spring.io/projects/spring-webflow)
- [Spring Web Flow reference](https://docs.spring.io/spring-webflow/docs/4.0.0/reference/html/)
