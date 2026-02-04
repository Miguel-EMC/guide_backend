# 28 - Spring Vault

Spring Vault is a project that provides Spring concepts to HashiCorp's Vault. It provides client-side support for securely storing and accessing secrets.

To use Spring Vault, you will need to add the `spring-vault-core` dependency to your project.

Once you have added the dependency, you can configure Spring Vault by providing the Vault endpoint and authentication information in your `application.properties` file.

Here is an example of how to configure Spring Vault:

```properties
spring.cloud.vault.uri=https://localhost:8200
spring.cloud.vault.token=...
```

Once you have configured Spring Vault, you can inject a `VaultTemplate` bean into your application and use it to interact with Vault.
