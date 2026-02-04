# 27 - Spring Statemachine

Spring Statemachine is a framework that allows you to use traditional state machine concepts with Spring applications. It provides a simple and extensible way to build your own state machines.

To use Spring Statemachine, you will need to add the `spring-statemachine-core` dependency to your project.

Once you have added the dependency, you can create a state machine by creating a `@Configuration` class and annotating it with `@EnableStateMachine`.

Here is an example of a simple state machine:

```java
@Configuration
@EnableStateMachine
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<States, Events> {

    @Override
    public void configure(StateMachineStateConfigurer<States, Events> states) throws Exception {
        states
            .withStates()
                .initial(States.SI)
                .states(EnumSet.allOf(States.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<States, Events> transitions) throws Exception {
        transitions
            .withExternal()
                .source(States.SI).target(States.S1).event(Events.E1)
                .and()
            .withExternal()
                .source(States.S1).target(States.S2).event(Events.E2);
    }
}
```
