# 28 - Spring Vault

Spring Vault provides Spring-style client support for HashiCorp Vault. It offers `VaultTemplate`, authentication support, and helper APIs to interact with secret engines. The current reference line is 4.0.0.

For Spring Boot applications, Spring Cloud Vault builds on Spring Vault to expose Vault as a configuration source using the Config Data API.

## When to Use
- Centralized secret management for credentials, API keys, and certificates
- Rotating secrets and dynamic credentials
- Externalized configuration without storing secrets in source control

## Dependencies

### Spring Vault (core client)
```xml
<dependency>
  <groupId>org.springframework.vault</groupId>
  <artifactId>spring-vault-core</artifactId>
</dependency>
```

### Spring Cloud Vault (Boot config integration)
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
```

## Configuration (Spring Cloud Vault)
```yaml
spring:
  application:
    name: billing-service
  config:
    import: "vault://"
  cloud:
    vault:
      scheme: https
      host: vault.mycorp.internal
      port: 8200
      authentication: TOKEN
      token: ${VAULT_TOKEN}
```

## Authentication Options
Spring Vault and Spring Cloud Vault support multiple authentication mechanisms such as Token, AppRole, AWS, Kubernetes, and more.

AppRole example:
```yaml
spring:
  cloud:
    vault:
      authentication: APPROLE
      app-role:
        role-id: ${VAULT_ROLE_ID}
        secret-id: ${VAULT_SECRET_ID}
```

## Reading Secrets with VaultTemplate
```java
@Service
public class SecretsService {
    private final VaultTemplate vaultTemplate;

    public SecretsService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    public Map<String, Object> readKv(String path) {
        VaultResponse response = vaultTemplate.read(path);
        return response == null ? Map.of() : response.getData();
    }
}
```

## Health and Observability
Spring Cloud Vault exposes a Vault health indicator and can be disabled with `management.health.vault.enabled=false`.

## Security Notes
- Static tokens are easy to start with but should be treated as sensitive and rotated; prefer AppRole or Kubernetes auth for production.

## References
- [Spring Vault project page](https://spring.io/projects/spring-vault)
- [Spring Vault reference](https://docs.spring.io/spring-vault/reference/)
- [Spring Cloud Vault project page](https://spring.io/projects/spring-cloud-vault)
- [Spring Cloud Vault reference](https://docs.spring.io/spring-cloud-vault/reference/)
