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

# What's New in Surefire 3.6.0

## Overview

Apache Maven Surefire 3.6.0 introduces a **major architectural simplification**: all test frameworks now execute
through a single unified [JUnit Platform](https://junit.org/junit5/docs/current/user-guide/#overview-what-is-junit-5)
provider (`surefire-junit-platform`). The five legacy providers have been removed, reducing maintenance complexity
and enabling a consistent experience across JUnit 5, JUnit 4, and TestNG.

This page summarizes the key changes and provides a migration guide for users upgrading from Surefire 3.5.x.

## Unified Provider Architecture

Starting with 3.6.0, **one provider runs all tests**:

| Test Framework | Execution Engine | Minimum Version |
|----------------|------------------|-----------------|
| **JUnit 5** (Jupiter) | Jupiter Engine (native) | 5.x |
| **JUnit 4** | [Vintage Engine](https://junit.org/junit5/docs/current/user-guide/#migrating-from-junit4-running) | **4.12** |
| **JUnit 3** tests | Vintage Engine (via JUnit 4 compatibility) | Requires JUnit **4.12**+ dependency |
| **TestNG** | [TestNG JUnit Platform Engine](https://github.com/junit-team/testng-engine) | **6.14.3** |

No explicit provider configuration is needed — Surefire auto-detects which engines are on the classpath
and delegates accordingly.

### Benefits

- **Single codebase** to maintain for listener, reporter, and launcher logic (previously 5 separate implementations)
- **Consistent behavior** across all test frameworks for filtering, parallel execution, and reporting
- **Easier contributions** — new features only need to be implemented once

## Breaking Changes

### Removed Providers

The following legacy provider modules have been removed:

- `surefire-junit3`
- `surefire-junit4`
- `surefire-junit47`
- `surefire-testng`

Only one provider module remains: `surefire-junit-platform` (the unified provider).

### Minimum Version Requirements

| Dependency | Minimum Version | Previous Minimum |
|------------|----------------|------------------|
| JUnit 4 | **4.12** | 3.8.1 |
| TestNG | **6.14.3** | 5.x |

- **JUnit 3**: No longer supported as a standalone dependency. JUnit 3 test code can still run, but requires
  a JUnit 4.12+ dependency on the classpath (the Vintage Engine handles the execution).
- **JUnit 4 versions before 4.12** are no longer supported.
- **TestNG versions before 6.14.3** are no longer supported.

### Reimplemented Stack Trace Handling

Stack trace handling has been completely reimplemented as part of the provider unification. The previous
per-provider implementations (`LegacyPojoStackTraceWriter`, `JUnit4StackTraceWriter`) have been replaced
by a new `StackTraceProvider` and `DefaultStackTraceWriter` that work uniformly across all test frameworks.
The new implementation includes configurable frame filtering and truncation.

## Migration Guide

### JUnit 5/6 Users

**No changes needed.** Your tests already run on the JUnit Platform natively.

### JUnit 4 Users (4.12+)

**No changes needed** in most cases. Surefire automatically adds the Vintage Engine and routes your tests
through the JUnit Platform. Verify your JUnit 4 dependency is version **4.12 or later**:

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
```

### JUnit 3 Users

Add or update to a JUnit 4.12+ dependency. Your JUnit 3 test code will continue to run unchanged
via the Vintage Engine:

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
```

### TestNG Users

Ensure your TestNG version is **6.14.3 or later**. Surefire will automatically use the
[TestNG JUnit Platform Engine](https://github.com/junit-team/testng-engine) to execute your tests:

```xml
<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <version>7.10.2</version>
    <scope>test</scope>
</dependency>
```

TestNG configuration (groups, listeners, suites, etc.) continues to work through Surefire's plugin
configuration and is mapped to the JUnit Platform infrastructure.

### Staying on Surefire 3.5.x

If you cannot update your test dependencies, you can stay on the 3.5.x line:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.5</version>
</plugin>
```

### Transitional: Using a Legacy Provider with 3.6.0

As a transitional measure, you can add a legacy provider from the 3.5.x line as a plugin dependency:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.6.0</version>
    <dependencies>
        <dependency>
            <groupId>org.apache.maven.surefire</groupId>
            <artifactId>surefire-junit4</artifactId>
            <version>3.5.5</version>
        </dependency>
    </dependencies>
</plugin>
```

Note: this backward compatibility may not be maintained in future releases.

## Further Reading

- [Provider Selection](examples/providers.html) — Details on the unified provider and legacy provider selection
- [Architecture Overview](architecture.html) — In-depth architecture documentation
- [PR #3179](https://github.com/apache/maven-surefire/pull/3179) — The primary pull request implementing the unified provider
