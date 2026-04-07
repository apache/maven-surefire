# Apache Maven Surefire - Copilot Instructions

## Build Commands

```bash
# Full build (unit tests only, no integration tests)
mvn clean install

# Full build with integration tests
mvn clean install -P run-its

# Build a single module
mvn clean install -pl surefire-api
mvn clean install -pl surefire-booter

# Skip tests during build
mvn clean install -DskipTests

# Run unit tests for a single module
mvn test -pl maven-surefire-common

# Run a single test class
mvn test -pl surefire-booter -Dtest=ForkedBooterTest

# Run a single test method
mvn test -pl surefire-booter -Dtest=ForkedBooterTest#testMethod

# Run a single integration test (requires -P run-its)
mvn verify -pl surefire-its -Prun-its -Dit.test=JUnit47RedirectOutputIT -Dmaven.build.cache.enabled=false

# Build site documentation
mvn site -pl maven-surefire-plugin

# Checkstyle (inherited from maven-parent, suppressions in src/config/checkstyle-suppressions.xml)
mvn checkstyle:check
```

## Architecture

### Module Dependency Flow

```
maven-surefire-plugin / maven-failsafe-plugin  (Maven Mojos - entry points)
        │
        ▼
maven-surefire-common  (AbstractSurefireMojo - shared Mojo logic)
        │
        ├──▶ surefire-booter  (ForkedBooter - JVM fork entry point)
        │
        ├──▶ surefire-api  (Provider SPI, report API, stream protocol)
        │
        ├──▶ surefire-extensions-api / surefire-extensions-spi
        │
        └──▶ surefire-providers/surefire-junit-platform  (unified test execution)
```

### Forked JVM Architecture

Surefire executes tests in a **forked JVM** separate from the Maven process. Understanding this split is essential:

- **Maven side** (`maven-surefire-common`): `AbstractSurefireMojo` configures and launches the fork. `booterclient/` handles communication with the forked process.
- **Forked side** (`surefire-booter`): `ForkedBooter.main()` is the entry point. It deserializes configuration, loads the provider, and runs tests. Communicates results back via an event-based binary stream protocol.
- **Shared contract** (`surefire-api`): Defines the `SurefireProvider` SPI, report events, and the stream protocol used between Maven and forked processes.

### Shading Strategy

Two modules exist solely for classpath isolation:

- **`surefire-shared-utils`**: Shades commons-lang3, commons-io, commons-compress, and maven-shared-utils into `org.apache.maven.surefire.shared.*` to avoid version conflicts with user projects.
- **`surefire-shadefire`**: Shades the entire surefire-junit-platform provider (plus surefire-api, surefire-booter) into `org.apache.maven.shadefire.*` so Surefire can test **itself** without classpath conflicts during its own build.

### Provider Model

All test frameworks execute through the JUnit Platform provider (`surefire-providers/surefire-junit-platform`):

- **JUnit 5**: Runs natively via Jupiter Engine
- **JUnit 4** (4.12+): Runs via Vintage Engine
- **TestNG** (6.14.3+): Runs via TestNG JUnit Platform Engine

Legacy providers (`surefire-junit3`, `surefire-junit4`, `surefire-junit47`, `surefire-testng`) still exist in the tree but are being consolidated.

### Integration Tests

`surefire-its` contains integration tests that launch real Maven builds against fixture projects in `surefire-its/src/test/resources/`. These require the `run-its` profile and test the full fork lifecycle end-to-end. They use `maven-verifier` to invoke Maven and assert on build output.

## Key Conventions

### Java Version

Source and target is **Java 8**. The `animal-sniffer-maven-plugin` enforces the `java18` API signature. JDK 9+ APIs must be accessed via reflection (see `ProcessHandleChecker` for an example using `ReflectionUtils`).

A `jdk9+` profile auto-activates on JDK 9+ to add `--add-opens` flags for test execution.

### Reflection Utilities

When accessing APIs not available at compile time (e.g., Java 9+ APIs), use `ReflectionUtils` from `surefire-api` rather than raw reflection:

- `tryLoadClass(classLoader, className)` → returns `null` on failure
- `tryGetMethod(clazz, name, params)` → returns `null` on failure
- `invokeMethodWithArray(target, method, args)` → wraps exceptions in `SurefireReflectionException`

### IDE Setup

Before importing into an IDE, run:

```bash
mvn install -P ide-development -f surefire-shared-utils/pom.xml
mvn compile -f surefire-grouper/pom.xml
```

The `ide-development` profile resolves IntelliJ IDEA artifact classifier issues. The `surefire-grouper` module needs a compile pass to generate JavaCC sources in `target/generated-sources/javacc`.

### Formatting

Follows `.editorconfig`: 4-space indentation for Java, 2-space for XML. Checkstyle rules are inherited from the Maven parent POM with project-specific suppressions in `src/config/checkstyle-suppressions.xml`.

### Test Isolation

Surefire uses a **different version of itself** to run its own tests (see `maven-surefire-plugin` version in `<pluginManagement>` vs `${project.version}`). The `surefire-shadefire` module enables this self-testing. The surefire plugin configuration sets `useSystemClassLoader=false` to isolate the version under test.
