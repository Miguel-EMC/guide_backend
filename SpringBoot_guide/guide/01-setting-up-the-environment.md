# 01 - Setting Up the Environment

To get started with Spring Boot, you will need to have the following installed on your system:

*   **Java Development Kit (JDK):** Spring Boot 3 requires Java 17 or later. You can download the JDK from the Oracle website or use an open-source distribution like OpenJDK. A popular choice is [Eclipse Temurin](https://adoptium.net/).

    To verify your installation, open a terminal and run the following command:
    ```bash
    java -version
    ```
    You should see output similar to the following:
    ```
    openjdk version "17.0.2" 2022-01-18
    OpenJDK Runtime Environment (build 17.0.2+8-86)
    OpenJDK 64-Bit Server VM (build 17.0.2+8-86, mixed mode, sharing)
    ```

*   **Maven or Gradle:** These are build automation tools that are used to manage your project's dependencies and build your application. You can download Maven from the [Apache Maven website](https://maven.apache.org/download.cgi) and Gradle from the [Gradle website](https://gradle.org/install/).

    To verify your Maven installation, run the following command:
    ```bash
    mvn -version
    ```
    You should see output similar to the following:
    ```
    Apache Maven 3.8.4 (22b128a6b22b3c3785efde4fb0d43a7b4941a166)
    Maven home: /opt/maven
    Java version: 17.0.2, vendor: Eclipse Adoptium, runtime: /opt/java/17
    Default locale: en_US, platform encoding: UTF-8
    OS name: "linux", version: "5.15.0-46-generic", arch: "amd64", family: "unix"
    ```
    To verify your Gradle installation, run the following command:
    ```bash
    gradle -version
    ```

*   **An IDE:** You can use any IDE that you are comfortable with, but we recommend using one that has good support for Spring Boot, such as:
    *   **IntelliJ IDEA:** The community edition is free and has excellent support for Spring Boot. The Ultimate edition has even more features.
    *   **Spring Tools Suite (STS):** This is a free, Eclipse-based IDE that is specifically designed for Spring development.
    *   **Visual Studio Code:** This is a free, lightweight code editor that has a number of extensions for Spring development, including the Spring Boot Extension Pack.

## Spring Initializr

The Spring Initializr is a web-based tool that you can use to generate a new Spring Boot project. It is a great way to get started with a new project, as it allows you to choose the dependencies that you want to use and it will generate all of the necessary boilerplate code for you.

You can access the Spring Initializr at [start.spring.io](https://start.spring.io/).