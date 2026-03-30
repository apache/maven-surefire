# Apache Maven Surefire - Copilot Instructions

This repository contains Apache Maven Surefire - a test framework project providing the `maven-surefire-plugin`, `maven-failsafe-plugin`, and `maven-surefire-report-plugin`.

## Build Commands

```bash
# Full build (unit tests only)
mvn clean install

# Full build with integration tests
mvn clean install -Prun-its

# Build a single module
mvn clean install -pl surefire-api
mvn clean install -pl :maven-surefire-report-plugin -am   # -am builds dependencies too

# Skip tests
mvn clean install -DskipTests

# Run unit tests for a single module
mvn test -pl maven-surefire-common

# Run a single test class
mvn test -pl surefire-booter -Dtest=ForkedBooterTest

# Run a single test method
mvn test -pl surefire-booter -Dtest=ForkedBooterTest#testMethod

# Run a single integration test (requires -Prun-its)
mvn verify -pl surefire-its -Prun-its -Dit.test=JUnit47RedirectOutputIT -Dmaven.build.cache.enabled=false

# Checkstyle
mvn checkstyle:check

# Build site documentation
mvn site -pl maven-surefire-plugin
```

## High-Level Architecture

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

**Other modules:**
- `surefire-report-parser` + `maven-surefire-report-plugin` (reporting)
- `surefire-its` (integration tests)
- `surefire-shared-utils` (shaded dependencies)
- `surefire-shadefire` (self-testing support)

### Forked JVM Architecture

Tests execute in a **forked JVM** separate from the Maven process:

- **Maven side** (`maven-surefire-common`): `AbstractSurefireMojo` configures and launches the fork. `booterclient/` handles communication.
- **Forked side** (`surefire-booter`): `ForkedBooter.main()` is the entry point. Deserializes configuration, loads the provider, runs tests. Results flow back via binary stream protocol.
- **Shared contract** (`surefire-api`): Defines the `SurefireProvider` SPI, report events, and stream protocol.

### Provider Model

All test frameworks execute through the unified JUnit Platform provider (`surefire-providers/surefire-junit-platform`):
- **JUnit 5**: Natively via Jupiter Engine
- **JUnit 4** (4.12+): Via Vintage Engine  
- **TestNG** (6.14.3+): Via TestNG JUnit Platform Engine

### Shading Strategy

- **`surefire-shared-utils`**: Shades commons-lang3, commons-io, commons-compress, maven-shared-utils into `org.apache.maven.surefire.shared.*` to avoid classpath conflicts.
- **`surefire-shadefire`**: Shades the junit-platform provider so Surefire can test itself without classpath conflicts. The surefire plugin config sets `useSystemClassLoader=false` for test isolation.

### Integration Tests

`surefire-its` contains integration tests that launch real Maven builds against fixture projects in `surefire-its/src/test/resources/`. Requires `-Prun-its`. Uses `maven-verifier` to invoke Maven and assert on build output.

## Key Conventions

### Java Version
- Source/target is **Java 8**
- The `animal-sniffer-maven-plugin` enforces the `java18` API signature
- JDK 9+ APIs must be accessed via reflection (see `ProcessHandleChecker` using `ReflectionUtils`)

### Reflection Utilities
When accessing APIs not available at compile time, use `ReflectionUtils` from `surefire-api`:
- `tryLoadClass(classLoader, className)` → returns `null` on failure
- `tryGetMethod(clazz, name, params)` → returns `null` on failure  
- `invokeMethodWithArray(target, method, args)` → wraps exceptions in `SurefireReflectionException`

### IDE Setup
Before importing into an IDE:
```bash
mvn install -P ide-development -f surefire-shared-utils/pom.xml
```
The `ide-development` profile resolves IntelliJ artifact classifier issues.

### Code Formatting
- **Java**: 4-space indentation
- **XML**: 2-space indentation
- Settings defined in `.editorconfig`
- Checkstyle rules inherited from Maven parent POM with suppressions in `src/config/checkstyle-suppressions.xml`

### Unit Test Patterns

Report plugin tests (`maven-surefire-report-plugin`) use the `@MojoTest`/`@InjectMojo` framework:
- `@MojoTest(realRepositorySession = true)` on the test class
- `@Basedir("/unit")` sets basedir to `src/test/resources/unit`
- `@InjectMojo(goal = "report", pom = "test-dir/plugin-config.xml")` injects the mojo
- `@Inject MavenProject mavenProject` for project access
- `getVariableValueFromObject(mojo, "field")` (static import from `MojoExtension`) to read mojo fields
- `plugin-config.xml` paths use `${basedir}` which resolves to the `@Basedir` path
- Stub `project implementation` classes in plugin-config.xml must be commented out (they NPE with `@MojoTest`)

## Development Requirements

- **Maven**: 3.6.3+
- **JDK**: 8+
- **Memory requirements** for release testing:
  ```bash
  # Linux/Unix
  export MAVEN_OPTS="-server -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:SoftRefLRUPolicyMSPerMB=50 -Djava.awt.headless=true"
  
  # Windows
  set MAVEN_OPTS="-server -Xmx256m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:SoftRefLRUPolicyMSPerMB=50 -Djava.awt.headless=true"
  ```