# Spring Boot Sample API

Simple Spring Boot REST API exposing a single `GET /api/hello` endpoint that returns a JSON message.

## Prerequisites
- Java 17+
- Maven 3.9+

## Run the app
```bash
mvn spring-boot:run
```
Visit http://localhost:8080/api/hello.

## Run tests
```bash
mvn test
```

## CI
- GitHub Actions workflow at `.github/workflows/ci.yml` runs `mvn -B -ntp verify` on push and PR targeting `main`.

## Build a jar
```bash
mvn clean package
```
The runnable jar will be at `target/springboot-sample-api-0.0.1-SNAPSHOT.jar`.
