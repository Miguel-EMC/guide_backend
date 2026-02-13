# 27 - Spring Statemachine

Spring Statemachine brings classic state machine concepts to Spring applications. The current reference line is 4.0.1 and it includes features such as hierarchical states, regions, guards, actions, listeners, and persistence support.

## When to Use
- Complex workflows with many discrete states and transitions
- Business processes that require strict state enforcement
- Processes that need persistence, auditability, or external triggers

## Dependencies
Spring Statemachine provides a BOM and multiple modules including core, starter, test, and persistence integrations.

### Maven (recommended with BOM)
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.statemachine</groupId>
      <artifactId>spring-statemachine-bom</artifactId>
      <version>4.0.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.springframework.statemachine</groupId>
    <artifactId>spring-statemachine-starter</artifactId>
  </dependency>
</dependencies>
```

### Gradle
```gradle
implementation platform("org.springframework.statemachine:spring-statemachine-bom:4.0.1")
implementation "org.springframework.statemachine:spring-statemachine-starter"
```

If you need persistence or tests, add modules such as `spring-statemachine-data-jpa`, `spring-statemachine-data-redis`, or `spring-statemachine-test`.

## Basic Order Workflow Example
```java
public enum States { NEW, PAID, FULFILLING, SHIPPED, CANCELED }
public enum Events { PAY, START_FULFILL, SHIP, CANCEL }
```

```java
@Configuration
@EnableStateMachine
public class OrderStateMachineConfig extends StateMachineConfigurerAdapter<States, Events> {

    @Override
    public void configure(StateMachineStateConfigurer<States, Events> states) throws Exception {
        states
            .withStates()
            .initial(States.NEW)
            .end(States.SHIPPED)
            .end(States.CANCELED)
            .states(EnumSet.allOf(States.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<States, Events> transitions) throws Exception {
        transitions
            .withExternal().source(States.NEW).target(States.PAID).event(Events.PAY)
            .and()
            .withExternal().source(States.PAID).target(States.FULFILLING).event(Events.START_FULFILL)
            .action(reserveInventory())
            .and()
            .withExternal().source(States.FULFILLING).target(States.SHIPPED).event(Events.SHIP)
            .and()
            .withExternal().source(States.NEW).target(States.CANCELED).event(Events.CANCEL).guard(notAlreadyShipped());
    }

    @Bean
    public Action<States, Events> reserveInventory() {
        return context -> {
            // reserve inventory or emit a domain event
        };
    }

    @Bean
    public Guard<States, Events> notAlreadyShipped() {
        return context -> context.getStateMachine().getState().getId() != States.SHIPPED;
    }

    @Bean
    public StateMachineListenerAdapter<States, Events> listener() {
        return new StateMachineListenerAdapter<>() {
            @Override
            public void transition(Transition<States, Events> transition) {
                // log or publish transition events
            }
        };
    }
}
```

```java
@Service
public class OrderWorkflow {
    private final StateMachine<States, Events> stateMachine;

    public OrderWorkflow(StateMachine<States, Events> stateMachine) {
        this.stateMachine = stateMachine;
    }

    public States send(Events event) {
        stateMachine.sendEvent(event);
        return stateMachine.getState().getId();
    }
}
```

## Persistence
Use a `StateMachinePersister` or a runtime persister to store and restore state between process restarts.

## Testing
The `spring-statemachine-test` module provides a test plan builder to validate transitions and state changes.

```java
@SpringBootTest
class OrderStateMachineTests {

    @Autowired
    private StateMachine<States, Events> stateMachine;

    @Test
    void happyPath() throws Exception {
        StateMachineTestPlan<States, Events> plan =
            StateMachineTestPlanBuilder.<States, Events>builder()
                .stateMachine(stateMachine)
                .step()
                    .expectStates(States.NEW)
                    .and()
                .step()
                    .sendEvent(Events.PAY)
                    .expectStateChanged(1)
                    .expectStates(States.PAID)
                    .and()
                .step()
                    .sendEvent(Events.START_FULFILL)
                    .expectStateChanged(1)
                    .expectStates(States.FULFILLING)
                    .and()
                .build();

        plan.test();
    }
}
```

## Notes
- Spring Statemachine open-source maintenance has ended; check the project update for guidance.

## References
- [Spring Statemachine project page](https://spring.io/projects/spring-statemachine)
- [Spring Statemachine reference](https://docs.spring.io/spring-statemachine/docs/4.0.1/reference/)
- [Spring Statemachine open source status update](https://spring.io/blog/2025/04/21/a-message-for-our-spring-statemachine-users)
