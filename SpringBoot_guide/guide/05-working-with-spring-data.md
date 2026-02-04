# 05 - Working with Spring Data

Spring Data is a sub-project of the Spring Framework that provides a familiar and consistent, Spring-based programming model for data access while still retaining the special traits of the underlying data store.

It makes it easier to use data access technologies, relational and non-relational databases, map-reduce frameworks, and cloud-based data services.

Some of the advantages of using Spring Data are:

*   **Powerful repository and custom object-mapping abstractions:** You can create repositories by simply extending the `Repository` interface.
*   **Dynamic query derivation from repository method names:** You can create queries by simply defining methods in your repository interface.
*   **Implementation domain base classes providing basic properties:** You can extend the `AbstractPersistable` class to provide your entities with basic properties, such as an ID.
*   **Support for transparent auditing (created, last changed):** You can use the `@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`, and `@LastModifiedDate` annotations to automatically audit your entities.
*   **Possibility to integrate custom repository code:** You can add your own custom methods to your repositories.
*   **Easy Spring integration with custom namespaces:** You can easily configure Spring Data in your Spring application.
