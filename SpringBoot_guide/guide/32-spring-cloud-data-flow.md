# 32 - Spring Cloud Data Flow

Spring Cloud Data Flow is a toolkit for building data integration and real-time data processing pipelines. It provides a set of tools for creating, deploying, and managing data pipelines.

To use Spring Cloud Data Flow, you will need to download and install the Spring Cloud Data Flow server. Once you have installed the server, you can create a data pipeline by creating a stream definition.

Here is an example of a simple stream definition that reads data from an HTTP source and writes it to a log sink:

```
http --server.port=9000 | log
```
