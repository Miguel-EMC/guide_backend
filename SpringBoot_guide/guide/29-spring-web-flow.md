# 29 - Spring Web Flow

Spring Web Flow is a project that provides a way to define and manage the flow of a web application. It is a good choice for applications that have complex and conversational UIs.

To use Spring Web Flow, you will need to add the `spring-webflow` dependency to your project.

Once you have added the dependency, you can create a flow by creating an XML file.

Here is an example of a simple flow that has two views:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<flow xmlns="http://www.springframework.org/schema/webflow"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.springframework.org/schema/webflow
                          http://www.springframework.org/schema/webflow/spring-webflow.xsd">

    <view-state id="view1">
        <transition on="next" to="view2"/>
    </view-state>

    <view-state id="view2">
        <transition on="previous" to="view1"/>
    </view-state>

</flow>
```
