<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Agent Guide for maven-surefire

This repository contains Apache Maven Surefire - a test framework project providing the `maven-surefire-plugin`, `maven-failsafe-plugin`, and `maven-surefire-report-plugin`.

This file is read by automated agents (security scanners, code
analyzers, AI assistants) operating on this repository.

## Security

Security model: [SECURITY.md](./SECURITY.md)

Agents that scan this repository should consult `SECURITY.md` and the
threat model it links before reporting issues.

Points at the Apache Maven family umbrella security model.

## Build Commands

Use `verify`, not `install`. In a reactor build each module gets the jars its sibling modules just
built, so nothing has to be installed in `~/.m2` first. CI runs `mvn clean verify -Prun-its` as well.

```bash
# Full build (unit tests only)
mvn clean verify

# Full build with integration tests
mvn clean verify -Prun-its

# Build one module and the modules it depends on (-am)
mvn verify -pl surefire-api -am
mvn verify -pl :maven-surefire-report-plugin -am

# Skip tests
mvn clean verify -DskipTests

# Run a single test class
# (failIfNoSpecifiedTests=false lets the upstream modules pass when nothing matches there)
mvn verify -pl surefire-booter -am -Dtest=ForkedBooterTest -Dsurefire.failIfNoSpecifiedTests=false

# Run a single test method
mvn verify -pl surefire-booter -am -Dtest=ForkedBooterTest#testMethod -Dsurefire.failIfNoSpecifiedTests=false

# Run a single integration test (-Dtest=none skips the unit tests of the upstream modules)
mvn verify -pl surefire-its -am -Prun-its -Dit.test=JUnit447RedirectOutputIT -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false

# Format the code and run Checkstyle
mvn spotless:apply
mvn checkstyle:check

# Build site documentation
mvn verify site -pl maven-surefire-plugin -am -DskipTests
```

### Build Gotchas

- **Always use `-am` with `-pl`.** Without it Maven takes the sibling modules from `~/.m2`. That copy
  can be an old snapshot or missing, and you end up testing against stale code without noticing.
- **Run up to `verify` (or at least `package`), not `compile` or `test`, when building more than one module.**
  `surefire-shared-utils` has no sources. Its relocated classes (`org.apache.maven.surefire.shared.*`)
  only exist once the shade plugin runs at `package`. So `mvn test -pl surefire-booter -am` fails with
  `package org.apache.maven.surefire.shared.lang3 does not exist`.
- **`-Prun-its` still writes to the local repository.** In `pre-integration-test`, `maven-invoker-plugin:install`
  copies the plugins, `surefire-junit-platform`, `surefire-shadefire` and `maven-surefire-report-plugin`
  into `${settings.localRepository}` so the IT projects can find them. If a new IT needs another artifact
  that `surefire-its` does not depend on, add it to `extraArtifacts` in `surefire-its/pom.xml`.
- **`-D` options on the outer command line do not reach the Maven builds the ITs launch.**
- **Every build runs Spotless and Checkstyle in `process-sources`.** Locally Spotless applies its formatting,
  so expect it to reformat the files you touched.
- **`install` is only needed** for the IDE setup below, or when another project has to use your snapshot.

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

`surefire-its` contains integration tests that launch real Maven builds against fixture projects in `surefire-its/src/test/resources/`. Requires `-Prun-its`. Uses `maven-executor` (via the `fixture.Verifier` wrapper) to invoke Maven and assert on build output. The profile installs what those builds need into the local repository first, so `verify` is enough (see Build Gotchas).

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

### Parallel Test Execution

Unit tests run with **JUnit 5 parallel execution enabled** (configured in the root `pom.xml`):
- `junit.jupiter.execution.parallel.mode.classes.default=concurrent` → test **classes run concurrently**
- `junit.jupiter.execution.parallel.mode.default=same_thread` → methods within a class stay on one thread

Because classes run in parallel, any test that mutates **JVM-global state must not race** with tests that read it, or you get flaky, OS/timing-dependent failures (often surfacing only on Windows CI). Annotate such classes with `@Isolated` (`org.junit.jupiter.api.parallel.Isolated`) so they run alone:
- Tests calling `System.setProperty` / `System.clearProperty` (e.g., `JstackTimeoutExtensionTest`)
- Tests mutating global static configuration (e.g., `StackTraceProviderTest`, `ForkedBooterTest`)

Note `StatelessXmlReporterTest` asserts against the full `System.getProperties()` snapshot, so any concurrent system-property mutation elsewhere will break it.

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
