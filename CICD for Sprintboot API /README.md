# CI/CD for Spring Boot Sample API

This folder contains a simple Spring Boot REST API (`springboot-sample-api`) and its GitHub Actions CI/CD pipeline.

## Pipeline overview (`.github/workflows/ci.yml`)
- Triggers: `push` to `main`, all `pull_request`, manual `workflow_dispatch`.
- Concurrency: only one run per ref; newer runs cancel older.
- Jobs:
  - **Build & Test**: `mvn -B -ntp verify` on JDK 17, uploads Surefire reports.
  - **Package JAR**: reuses Maven cache, builds with `-DskipTests`, uploads the built JAR artifact.
  - **Build & Push Image**: builds and pushes a Docker image.
- Caching: Maven dependency cache via `actions/setup-java`.
- Artifacts: test reports (`surefire-reports`) and packaged JAR (`target/*.jar`).

## Local development
```bash
cd springboot-sample-api
mvn -B -ntp verify      # tests
mvn -B -ntp package     # build JAR
mvn spring-boot:run     # run locally on :8080
```

## Consuming CI artifacts
- Download the JAR artifact from the workflow run summary (named `springboot-sample-api-jar`).
- Test reports are under the `surefire-reports` artifact.

## Docker image conventions
- Image name: `<DOCKERHUB_USERNAME>/springboot-sample-api`
- Tags pushed: `latest` and the commit SHA.

## Client-facing tasks (what was delivered)
- Added GitHub Actions workflow for automated build, test, package, and Docker publish.
- Documented required secrets and how to run the pipeline locally.
- Produced artifacts (JAR and test reports) for each run.
