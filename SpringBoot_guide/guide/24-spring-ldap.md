# 24 - Spring LDAP

Spring LDAP simplifies LDAP access in Spring applications with template APIs and repository support.

## When to Use
- Centralized user or group directories
- Enterprise identity stores exposed via LDAP
- Integration with legacy systems that require LDAP lookups

## Dependencies
### Maven
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-ldap</artifactId>
</dependency>
```

### Gradle
```gradle
implementation "org.springframework.boot:spring-boot-starter-data-ldap"
```

If you are not using Spring Boot, add `spring-ldap-core` directly.

## Spring Boot Configuration
Spring Boot auto-configures `LdapContextSource` and `LdapTemplate` when `spring.ldap.*` properties are set.

```yaml
spring:
  ldap:
    urls: "ldap://localhost:389"
    base: "dc=example,dc=com"
    username: "cn=admin,dc=example,dc=com"
    password: "secret"
```

You can customize the LDAP environment with `spring.ldap.base-environment` and set a default base with `spring.ldap.base`.

## Using LdapTemplate
```java
@Service
public class DirectoryService {
    private final LdapTemplate ldapTemplate;

    public DirectoryService(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public boolean authenticate(String username, String password) {
        return ldapTemplate.authenticate("", "(uid=" + username + ")", password);
    }
}
```

## Repositories (Spring Data LDAP)
Enable repositories and scan LDAP entities:
```java
@Configuration
@EnableLdapRepositories
@EntityScan(basePackageClasses = Person.class)
public class LdapConfig {
}
```

```java
@Entry(base = "ou=people", objectClasses = {"inetOrgPerson"})
public class Person {
    @Id
    private Name id;

    @Attribute(name = "uid")
    private String uid;

    @Attribute(name = "cn")
    private String fullName;

    @Attribute(name = "sn")
    private String lastName;
}
```

```java
public interface PersonRepository extends LdapRepository<Person> {
    Optional<Person> findByUid(String uid);
}
```

## Embedded LDAP for Tests
Spring Boot can auto-configure an embedded LDAP server for tests using `spring.ldap.embedded.*` properties, backed by UnboundID.

## References
- [Spring Boot LDAP auto-configuration reference](https://docs.spring.io/spring-boot/reference/data/nosql.html#data.nosql.ldap)
- [Spring LDAP project page](https://spring.io/projects/spring-ldap)
- [Spring LDAP reference](https://docs.spring.io/spring-ldap/reference/)
